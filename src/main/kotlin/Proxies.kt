package edu.illinois.cs.cs125.answerable

import javassist.util.proxy.Proxy
import javassist.util.proxy.ProxyFactory
import org.apache.bcel.Const
import org.apache.bcel.Repository
import org.apache.bcel.classfile.*
import org.apache.bcel.generic.*
import org.objenesis.ObjenesisStd
import org.objenesis.instantiator.ObjectInstantiator
import java.lang.reflect.Modifier
import java.util.*

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
        childClass.getPublicFields().forEach { it.set(forward, self.javaClass.getField(it.name).get(self)) }
        val result = childClass.getMethod(method.name, *method.parameterTypes).invoke(forward, *args)
        childClass.getPublicFields().forEach { self.javaClass.getField(it.name).set(self, it.get(forward)) }
        result
    }

    return subProxy
}

private fun Class<*>.slashName() = canonicalName.replace('.', '/')

internal fun mkGeneratorMirrorClass(referenceClass: Class<*>, targetClass: Class<*>): Class<*> {
    return mkMirrorClass(referenceClass, referenceClass, targetClass, "answerablemirror." + UUID.randomUUID().toString().replace("-", ""))
}

private fun mkMirrorClass(baseClass: Class<*>, referenceClass: Class<*>, targetClass: Class<*>, mirrorName: String): Class<*> {
    val refLName = "L${referenceClass.slashName()};"
    val subLName = "L${targetClass.slashName()};"
    val mirrorSlashName = mirrorName.replace('.', '/')
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
    val atVerifyName = baseClass.declaredMethods.firstOrNull { it.isAnnotationPresent(Verify::class.java) }?.name

    val classGen = ClassGen(Repository.lookupClass(baseClass))

    val constantPoolGen = classGen.constantPool
    val constantPool = constantPoolGen.constantPool
    val newClassIdx = constantPoolGen.addClass(targetClass.canonicalName)
    val mirrorClassIdx = constantPoolGen.addClass(mirrorSlashName)
    classGen.classNameIndex = mirrorClassIdx

    for (i in 1 until constantPoolGen.size) {
        val constant = constantPoolGen.getConstant(i)
        if (constant is ConstantCP) {
            if (constant.classIndex == 0) continue
            val className = constant.getClass(constantPool)
            if (className == referenceClass.canonicalName) {
                var shouldReplace = false
                val memberName = (constantPool.getConstant(constant.nameAndTypeIndex) as ConstantNameAndType).getName(constantPool)
                if (constant is ConstantMethodref) {
                    shouldReplace = !(referenceClass.declaredMethods.firstOrNull { Modifier.isStatic(it.modifiers) && it.name == memberName }?.let { method ->
                        setOf(Helper::class.java, Generator::class.java, Next::class.java).any { annotation -> method.isAnnotationPresent(annotation) }
                    } ?: false)
                } else if (constant is ConstantFieldref) {
                    shouldReplace = !(referenceClass.declaredFields.firstOrNull { Modifier.isStatic(it.modifiers) && it.name == memberName }?.isAnnotationPresent(Helper::class.java) ?: false)
                }
                constant.classIndex = if (shouldReplace) newClassIdx else mirrorClassIdx
            }
        } else if (constant is ConstantNameAndType) {
            val typeSignature = constant.getSignature(constantPool)
            if (typeSignature.contains(refLName)) {
                constantPoolGen.setConstant(constant.signatureIndex, ConstantUtf8(typeSignature.replace(refLName, subLName)))
            }
        } else if (constant is ConstantClass) {
            val name = constant.getBytes(constantPool)
            if (name.startsWith("${baseClass.slashName()}\$")) {
                constantPoolGen.setConstant(constant.nameIndex, ConstantUtf8(name.replace(baseClass.slashName(), mirrorSlashName)))
            }
        }
    }

    fun classIndexReplacement(currentIndex: Int): Int? {
        val classConst = constantPool.getConstant(currentIndex) as? ConstantClass ?: return null
        val className = constantPool.getConstant(classConst.nameIndex) as? ConstantUtf8 ?: return null
        return if (className.bytes == referenceClass.canonicalName.replace('.', '/')) {
            newClassIdx
        } else if (className.bytes.trimStart('[') == refLName) {
            val arrDims = className.bytes.length - className.bytes.trimStart('[').length
            constantPoolGen.addArrayClass(ArrayType(targetClass.canonicalName, arrDims))
        } else {
            currentIndex
        }
    }

    classGen.methods.forEach {
        classGen.removeMethod(it)
        if (!it.isStatic || it.name == atVerifyName) return@forEach

        val newMethod = MethodGen(it, classGen.className, constantPoolGen)
        newMethod.argumentTypes = it.argumentTypes.map(::fixType).toTypedArray()
        newMethod.returnType = fixType(it.returnType)
        newMethod.instructionList.map { handle -> handle.instruction }.filterIsInstance(CPInstruction::class.java).forEach { instr ->
            classIndexReplacement(instr.index)?.let { newIdx -> instr.index = newIdx }
        }

        newMethod.codeAttributes.filterIsInstance(StackMap::class.java).firstOrNull()?.let { stackMap ->
            stackMap.stackMap.forEach { stackEntry ->
                stackEntry.typesOfLocals.plus(stackEntry.typesOfStackItems).filter { local -> local.type == Const.ITEM_Object }.forEach { local ->
                    classIndexReplacement(local.index)?.let { newIdx -> local.index = newIdx }
                }
            }
        }
        newMethod.localVariables.forEach { localVariableGen ->
            localVariableGen.type = fixType(localVariableGen.type)
        }

        classGen.addMethod(newMethod.method)
    }

    classGen.attributes.filterIsInstance(InnerClasses::class.java).firstOrNull()?.innerClasses?.forEach { innerClass ->
        val outerName = (constantPool.getConstant(innerClass.outerClassIndex) as? ConstantClass)?.getBytes(constantPool)
        if (outerName == baseClass.slashName()) {
            innerClass.outerClassIndex = mirrorClassIdx
        }
    }

    classGen.javaClass.dump("Fiddled.class")
    return loader.loadBytes(mirrorName, classGen.javaClass.bytes)
}