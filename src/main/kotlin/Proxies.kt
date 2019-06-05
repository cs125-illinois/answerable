package edu.illinois.cs.cs125.answerable

import javassist.util.proxy.Proxy
import javassist.util.proxy.ProxyFactory
import org.apache.bcel.Repository
import org.apache.bcel.classfile.*
import org.apache.bcel.generic.*
import org.objenesis.ObjenesisStd
import org.objenesis.instantiator.ObjectInstantiator
import java.lang.reflect.Modifier

private val objenesis = ObjenesisStd()

private class BytesClassLoader : ClassLoader() {
    fun loadBytes(name: String, bytes: ByteArray): Class<*> {
        val clazz = defineClass(name, bytes, 0, bytes.size)
        resolveClass(clazz)
        return clazz
    }
}

private val loader = BytesClassLoader()

/*
 * For performance reasons, we want to re-use instantiators as much as possible.
 * A map is used for future-safety so that as many proxy instantiators as are needed can be created safely,
 * even if using only one is the most common use case.
 *
 * We map from 'superClass' instead of directly from 'proxyClass' as we won't have access to
 * the same reference to 'proxyClass' on future calls.
 */
private val proxyInstantiators: MutableMap<Class<*>, ObjectInstantiator<out Any?>> = mutableMapOf()

fun mkProxy(superClass: Class<*>, childClass: Class<*>, forward: Any): Any {
    // if we don't have an instantiator for this proxy class, make a new one
    val instantiator = proxyInstantiators[superClass] ?: run {
        val factory = ProxyFactory()

        factory.superclass = superClass
        factory.setFilter { it.name != "finalize" }
        val proxyClass = factory.createClass()

        objenesis.getInstantiatorOf(proxyClass).also { proxyInstantiators[superClass] = it }
    }
    val subProxy = instantiator.newInstance()

    (subProxy as Proxy).setHandler { self, method, _, args ->
        childClass.getPublicFields().forEach { self.javaClass.getField(it.name).set(self, it.get(forward)) }
        childClass.getMethod(method.name, *method.parameterTypes).invoke(forward, *args)
    }

    return subProxy
}

internal fun mkGeneratorMirrorClass(referenceClass: Class<*>, targetClass: Class<*>): Class<*> {
    val refLName = "L${referenceClass.canonicalName.replace('.','/')};"
    val subLName = "L${targetClass.canonicalName.replace('.','/')};"
    fun fixType(type: Type): Type {
        if (type.signature.trimStart('[') == refLName) {
            return if (type is ArrayType) {
                ArrayType(targetClass.canonicalName, type.dimensions)
            } else {
                ObjectType(targetClass.canonicalName)
            }
        }
        return type
    }
    val atVerifyName = referenceClass.declaredMethods.firstOrNull { it.isAnnotationPresent(Verify::class.java) }?.name

    val classGen = ClassGen(Repository.lookupClass(referenceClass))

    val constantPoolGen = classGen.constantPool
    val constantPool = constantPoolGen.constantPool
    val newClassIdx = constantPoolGen.addClass(targetClass.canonicalName)
    val newClassArrayIdx = Array(255) {
        if (it == 0) 0 else constantPoolGen.addArrayClass(ArrayType(targetClass.canonicalName, it))
    }
    for (i in 1 until constantPoolGen.size) {
        val constant = constantPoolGen.getConstant(i)
        if (constant is ConstantCP) {
            if (constant.classIndex == 0) continue
            val className = constant.getClass(constantPool)
            if (className == referenceClass.canonicalName) {
                var shouldReplace = false
                val memberName = (constantPool.getConstant(constant.nameAndTypeIndex) as ConstantNameAndType).getName(constantPool)
                if (constant is ConstantMethodref) {
                    shouldReplace = !(referenceClass.declaredMethods.firstOrNull { Modifier.isStatic(it.modifiers) && it.name == memberName }?.isAnnotationPresent(Helper::class.java) ?: false)
                } else if (constant is ConstantFieldref) {
                    shouldReplace = !(referenceClass.declaredFields.firstOrNull { Modifier.isStatic(it.modifiers) && it.name == memberName }?.isAnnotationPresent(Helper::class.java) ?: false)
                }
                if (shouldReplace) constant.classIndex = newClassIdx
            }
        } else if (constant is ConstantNameAndType) {
            val typeSignature = constant.getSignature(constantPool)
            if (typeSignature.contains(refLName)) {
                constantPoolGen.setConstant(constant.signatureIndex, ConstantUtf8(typeSignature.replace(refLName, subLName)))
            }
        }
    }

    classGen.methods.forEach {
        classGen.removeMethod(it)
        if (!it.isStatic || it.name == atVerifyName) return@forEach

        val newMethod = MethodGen(it, classGen.className, constantPoolGen)
        newMethod.argumentTypes = it.argumentTypes.map(::fixType).toTypedArray()
        newMethod.returnType = fixType(it.returnType)
        newMethod.instructionList.map { handle -> handle.instruction }.filterIsInstance(CPInstruction::class.java).forEach eachInstr@{ instr ->
            val classConst = constantPool.getConstant(instr.index) as? ConstantClass ?: return@eachInstr
            val className = constantPool.getConstant(classConst.nameIndex) as? ConstantUtf8 ?: return@eachInstr
            if (className.bytes == referenceClass.canonicalName.replace('.', '/')) {
                instr.index = newClassIdx
            } else if (className.bytes.trimStart('[') == refLName) {
                val arrDims = className.bytes.length - className.bytes.trimStart('[').length
                instr.index = newClassArrayIdx[arrDims]
            }
        }

        classGen.addMethod(newMethod.method)
    }

    classGen.javaClass.dump("Fiddled.class")
    return loader.loadBytes(referenceClass.canonicalName, classGen.javaClass.bytes)
}