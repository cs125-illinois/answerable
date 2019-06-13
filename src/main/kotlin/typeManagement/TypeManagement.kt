package edu.illinois.cs.cs125.answerable.typeManagement

import edu.illinois.cs.cs125.answerable.*
import javassist.util.proxy.Proxy
import javassist.util.proxy.ProxyFactory
import org.apache.bcel.Const
import org.apache.bcel.Repository
import org.apache.bcel.classfile.*
import org.apache.bcel.generic.*
import org.apache.bcel.generic.FieldOrMethod
import org.objenesis.ObjenesisStd
import org.objenesis.instantiator.ObjectInstantiator
import java.lang.IllegalStateException
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Member
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

/*
 * For performance reasons, we want to re-use instantiators as much as possible.
 * A map is used for future-safety so that as many proxy instantiators as are needed can be created safely,
 * even if using only one is the most common use case.
 *
 * We map from 'superClass' instead of directly from 'proxyClass' as we won't have access to
 * the same reference to 'proxyClass' on future calls.
 */
private val proxyInstantiators: MutableMap<Class<*>, ObjectInstantiator<out Any?>> = mutableMapOf()

/**
 * Creates a proxy to allow treating an object as an instance of a similarly-shaped class.
 * @param superClass the class that the object needs to appear as (be an instance of)
 * @param childClass the original class
 * @param forward an instance of childClass to which method calls will be forwarded
 * @return an instance (the proxy) of a subclass of superClass
 */
internal fun mkProxy(superClass: Class<*>, childClass: Class<*>, forward: Any): Any {
    return mkProxy(superClass, superClass, childClass, childClass, forward)
}

private data class TypeMap(
        val from: Class<*>,
        val to: Class<*>
)

/**
 * Determines what an inner class should be proxied to, if any.
 * @param outermostSuperClass the outer class a proxy is being made an instance of
 * @param childClass an inner class of the real/original class
 * @return a TypeMap that determines what classes to map fields and method between, or null if no proxy is needed
 */
private fun mostDerivedProxyableClass(outermostSuperClass: Class<*>, childClass: Class<*>?): TypeMap? {
    if (childClass == null) return null
    if (childClass.enclosingClass == null) return null
    val innerPath = childClass.name.split('$', limit = 2)[1]
    val correspondingSuper = "${outermostSuperClass.name}\$$innerPath"
    try {
        return TypeMap(to = Class.forName(correspondingSuper), from = childClass)
    } catch (e: ClassNotFoundException) {
        return mostDerivedProxyableClass(outermostSuperClass, childClass.superclass)
    }
}

private fun mkProxy(superClass: Class<*>, outermostSuperClass: Class<*>, childClass: Class<*>, outermostChildClass: Class<*>, forward: Any): Any {
    if (superClass == childClass) return forward

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
        if (result != null && result.javaClass.enclosingClass != null && result.javaClass.name.startsWith("${outermostChildClass.name}$")) {
            val innerMap = mostDerivedProxyableClass(outermostSuperClass, result.javaClass)
            if (innerMap == null) {
                result
            } else {
                val innerProxy = mkProxy(innerMap.to, outermostSuperClass, innerMap.from, outermostChildClass, result)
                innerMap.from.getPublicFields().forEach { innerProxy.javaClass.getField(it.name).set(innerProxy, it.get(result)) }
                innerProxy
            }
        } else {
            result
        }
    }

    return subProxy
}

private fun Class<*>.slashName() = name.replace('.', '/')

/**
 * Creates a mirror class containing only enough of the reference class to generate submission classes.
 * @param referenceClass the reference class containing methods to mirror
 * @param targetClass the class the generator should make instances of
 * @return a mirror class suitable only for generation
 */
internal fun mkGeneratorMirrorClass(referenceClass: Class<*>, targetClass: Class<*>): Class<*> {
    return mkMirrorClass(referenceClass, referenceClass, targetClass, "answerablemirror.m" + UUID.randomUUID().toString().replace("-", ""), mutableMapOf(), BytesClassLoader())
}

/**
 * Mirrors one class, which may be an inner class. (Recursive helper for mkGeneratorMirrorClass.)
 * @param baseClass the class to mirror
 * @param referenceClass the original, outermost reference class
 * @param targetClass the original, outermost submission class (to generate instances of)
 * @param mirrorName the desired fully-qualified dot name of the mirror class
 * @param mirrorsMade the map of names to finished mirrors
 * @param loader the class loader to load the mirror
 * @return the mirrored class
 */
private fun mkMirrorClass(baseClass: Class<*>, referenceClass: Class<*>, targetClass: Class<*>, mirrorName: String, mirrorsMade: MutableMap<String, Class<*>>, loader: BytesClassLoader): Class<*> {
    mirrorsMade[mirrorName]?.let { return it }

    val refLName = "L${referenceClass.slashName()};"
    val subLName = "L${targetClass.slashName()};"
    val mirrorSlashName = mirrorName.replace('.', '/')
    val refLBase = "L${referenceClass.slashName()}$"
    val mirrorLBase = "L${mirrorSlashName.split("$", limit = 2)[0]}$"
    fun fixType(type: Type): Type {
        val newName = if (type.signature.trimStart('[') == refLName) {
            targetClass.canonicalName
        } else if (type.signature.trimStart('[').startsWith(refLBase)) {
            type.signature.trimStart('[').trimEnd(';').replace(refLBase, mirrorLBase).trimStart('L')
        } else {
            return type
        }
        return if (type is ArrayType) {
            ArrayType(newName, type.dimensions)
        } else {
            ObjectType(newName)
        }
    }
    val atVerifyName = baseClass.declaredMethods.firstOrNull { it.isAnnotationPresent(Verify::class.java) }?.name

    val classGen = ClassGen(Repository.lookupClass(baseClass))
    Repository.clearCache() // Otherwise future mirrorings will fail

    val constantPoolGen = classGen.constantPool
    val constantPool = constantPoolGen.constantPool
    val newClassIdx = constantPoolGen.addClass(targetClass.canonicalName)
    val mirrorClassIdx = constantPoolGen.addClass(mirrorSlashName)
    val refMirrorClassIdx = constantPoolGen.addClass(mirrorSlashName.split("$", limit = 2)[0])
    classGen.classNameIndex = mirrorClassIdx

    fun fixOuterClassName(innerName: String): String {
        val topLevelMirrorName = mirrorName.split("$", limit = 2)[0]
        val innerPath = innerName.split("$", limit = 2)[1]
        return "$topLevelMirrorName\$$innerPath"
    }

    for (i in 1 until constantPoolGen.size) {
        val constant = constantPoolGen.getConstant(i)
        if (constant is ConstantCP && (constant is ConstantFieldref || constant is ConstantMethodref || constant is ConstantInterfaceMethodref)) {
            if (constant.classIndex == 0 || constantPool.getConstant(constant.classIndex) !is ConstantClass) continue
            val className = constant.getClass(constantPool)
            if (className == referenceClass.canonicalName) {
                var shouldReplace = false
                val memberName = (constantPool.getConstant(constant.nameAndTypeIndex) as ConstantNameAndType).getName(constantPool)
                if (constant is ConstantMethodref || constant is ConstantInterfaceMethodref) {
                    val helperAnnotations = setOf(Helper::class.java, Generator::class.java, Next::class.java, EdgeCase::class.java, SimpleCase::class.java)
                    shouldReplace = !(referenceClass.declaredMethods.firstOrNull { Modifier.isStatic(it.modifiers) && it.name == memberName }?.let { method ->
                        helperAnnotations.any { annotation -> method.isAnnotationPresent(annotation) }
                    } ?: false) && !memberName.contains('$')
                } else if (constant is ConstantFieldref) {
                    val helperAnnotations = setOf(Helper::class.java, EdgeCase::class.java, SimpleCase::class.java)
                    shouldReplace = !(referenceClass.declaredFields
                        .firstOrNull { Modifier.isStatic(it.modifiers) && it.name == memberName }?.let { field ->
                            helperAnnotations.any { annotation -> field.isAnnotationPresent(annotation) }
                        } ?: false)
                }
                constant.classIndex = if (shouldReplace) newClassIdx else refMirrorClassIdx
            } else if (className.startsWith("${referenceClass.canonicalName}$")) {
                constant.classIndex = constantPoolGen.addClass(fixOuterClassName(className).replace('.', '/'))
            }
        } else if (constant is ConstantNameAndType) {
            val typeSignature = constant.getSignature(constantPool)
            if (typeSignature.contains(refLName) || typeSignature.contains(refLBase)) {
                val fixedSignature = typeSignature.replace(refLName, subLName).replace(refLBase, mirrorLBase)
                constantPoolGen.setConstant(constant.signatureIndex, ConstantUtf8(fixedSignature))
            }
        } else if (constant is ConstantClass) {
            val name = constant.getBytes(constantPool)
            if (name.startsWith("${baseClass.slashName()}\$")) {
                val inner = Class.forName(name.replace('/', '.'))
                val innerMirror = mkMirrorClass(inner, referenceClass, targetClass, fixOuterClassName(name), mirrorsMade, loader)
                constantPoolGen.setConstant(constant.nameIndex, ConstantUtf8(innerMirror.slashName()))
            } else if (name.startsWith("${referenceClass.slashName()}\$")) {
                // Shouldn't merge this with the above condition because of possible mutual reference (infinite recursion)
                constantPoolGen.setConstant(constant.nameIndex, ConstantUtf8(fixOuterClassName(name).replace('.', '/')))
            }
        }
    }

    fun classIndexReplacement(currentIndex: Int): Int? {
        val classConst = constantPool.getConstant(currentIndex) as? ConstantClass ?: return null
        val className = (constantPool.getConstant(classConst.nameIndex) as? ConstantUtf8)?.bytes ?: return null
        val curType = if (className.startsWith("[")) Type.getType(className) else ObjectType(className)
        val newType = fixType(curType)
        return if (newType.signature == curType.signature) {
            currentIndex
        } else if (newType is ArrayType) {
            constantPoolGen.addArrayClass(newType)
        } else {
            constantPoolGen.addClass(newType as ObjectType)
        }
    }

    classGen.methods.forEach {
        classGen.removeMethod(it)
        if ((!it.isStatic || it.name == atVerifyName) && baseClass == referenceClass) return@forEach

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

    // classGen.javaClass.dump("Fiddled${mirrorsMade.size}.class") // Uncomment for debugging
    return loader.loadBytes(mirrorName, classGen.javaClass.bytes).also { mirrorsMade[mirrorName] = it }
}

/**
 * Throws AnswerableBytecodeVerificationException if a mirror of the given generator class would fail with an illegal or absent member access.
 * @param referenceClass the original, non-mirrored reference class
 */
internal fun verifyMemberAccess(referenceClass: Class<*>) {
    verifyMemberAccess(referenceClass, referenceClass, mutableSetOf(), mapOf())
}

/**
 * Verifies the given class, which may be an inner class. (Recursive helper for the above overload.)
 * @param currentClass the class to verify
 * @param referenceClass the original, outermost reference class
 * @param checked the collection of classes already verified
 * @param dangerousAccessors members whose access will cause the given problem
 */
private fun verifyMemberAccess(currentClass: Class<*>, referenceClass: Class<*>, checked: MutableSet<Class<*>>, dangerousAccessors: Map<String, AnswerableBytecodeVerificationException>) {
    if (checked.contains(currentClass)) return
    checked.add(currentClass)

    val toCheck = Repository.lookupClass(currentClass)
    val methodsToCheck = if (currentClass == referenceClass) {
        toCheck.methods.filter { it.annotationEntries.any {
            ae -> ae.annotationType in setOf(Generator::class.java.name, Next::class.java.name, Helper::class.java.name).map { t -> ObjectType(t).signature }
        } }.toTypedArray()
    } else {
        toCheck.methods
    }

    val constantPool = toCheck.constantPool
    val innerClassIndexes = toCheck.attributes.filterIsInstance(InnerClasses::class.java).firstOrNull()?.innerClasses?.filter { innerClass ->
        (constantPool.getConstant(innerClass.innerClassIndex) as ConstantClass).getBytes(constantPool).startsWith("${toCheck.className.replace('.', '/')}$")
    }?.map { it.innerClassIndex } ?: listOf()

    val dangersToInnerClasses = dangerousAccessors.toMutableMap()

    val methodsChecked = mutableSetOf<Method>()
    fun checkMethod(method: Method, checkInner: Boolean) {
        if (methodsChecked.contains(method)) return
        methodsChecked.add(method)

        InstructionList(method.code.code).map { it.instruction }.filterIsInstance(CPInstruction::class.java).forEach eachInstr@{ instr ->
            if (instr is FieldOrMethod) {
                if (instr is INVOKEDYNAMIC) return@eachInstr
                val refConstant = constantPool.getConstant(instr.index) as? ConstantCP ?: return@eachInstr
                if (refConstant.getClass(constantPool) != referenceClass.name) return@eachInstr
                val signatureConstant = constantPool.getConstant(refConstant.nameAndTypeIndex) as ConstantNameAndType
                if (instr is FieldInstruction) {
                    val field = try {
                        referenceClass.getDeclaredField(signatureConstant.getName(constantPool))
                    } catch (e: NoSuchFieldException) {
                        return@eachInstr
                    }
                    if (Modifier.isStatic(field.modifiers) && field.isAnnotationPresent(Helper::class.java)) return@eachInstr
                    if (!Modifier.isPublic(field.modifiers))
                        throw AnswerableBytecodeVerificationException(method.name, currentClass, field)
                } else if (instr is InvokeInstruction) {
                    val name = signatureConstant.getName(constantPool)
                    val signature = signatureConstant.getSignature(constantPool)
                    if (name == "<init>") {
                        referenceClass.declaredConstructors.filter { dc ->
                            !Modifier.isPublic(dc.modifiers)
                                    && signature == "(${dc.parameterTypes.joinToString(separator = "") { Type.getType(it).signature }})V"
                        }.forEach { candidate ->
                            throw AnswerableBytecodeVerificationException(method.name, currentClass, candidate)
                        }
                    } else {
                        referenceClass.declaredMethods.filter { dm ->
                            dm.name == name
                                    && !Modifier.isPublic(dm.modifiers)
                                    && Type.getSignature(dm) == signature
                                    && (setOf(Generator::class.java, Next::class.java, Helper::class.java).none { dm.isAnnotationPresent(it) } || !Modifier.isStatic(dm.modifiers))
                        }.forEach { candidate ->
                            dangerousAccessors[candidate.name]?.let { throw AnswerableBytecodeVerificationException(it, method.name, currentClass) }
                            if (!candidate.name.contains('$')) throw AnswerableBytecodeVerificationException(method.name, currentClass, candidate)
                        }
                    }
                }
            } else if (checkInner) {
                val classConstant = constantPool.getConstant(instr.index) as? ConstantClass ?: return@eachInstr
                if (innerClassIndexes.contains(instr.index)) {
                    verifyMemberAccess(Class.forName(classConstant.getBytes(constantPool).replace('/', '.')), referenceClass, checked, dangersToInnerClasses)
                }
            }
        }
    }

    if (referenceClass == currentClass) {
        toCheck.methods.filter { it.name.contains('$') }.forEach {
            try {
                checkMethod(it, false)
            } catch (e: AnswerableBytecodeVerificationException) {
                dangersToInnerClasses.put(it.name, e)
            }
        }
    }

    methodsToCheck.forEach { checkMethod(it, true) }
}

internal class AnswerableBytecodeVerificationException(val blameMethod: String, val blameClass: Class<*>, val member: Member) : AnswerableVerificationException("Bytecode error not specified. Please report a bug.") {

    override val message: String?
        get() {
            return "\nMirrorable method `$blameMethod' in ${describeClass(blameClass)} " +
                    when (member) {
                        is java.lang.reflect.Method -> "calls non-public submission method: ${MethodData(member)}"
                        is Field -> "uses non-public submission field: ${member.name}"
                        is Constructor<*> -> "uses non-public submission constructor: ${MethodData(member)}"
                        else -> throw IllegalStateException("AnswerableBytecodeVerificationException.member must be a Method or Field. Please report a bug.")
                    }
        }

    private fun describeClass(clazz: Class<*>): String {
        return "`${clazz.simpleName()}'" + (clazz.enclosingMethod?.let {
            " (inside `${it.name}' method of ${describeClass(clazz.enclosingClass)})"
        } ?: "")
    }

    constructor(fromInner: AnswerableBytecodeVerificationException, blameMethod: String, blameClass: Class<*>) : this(blameMethod, blameClass, fromInner.member) {
        initCause(fromInner)
    }

}