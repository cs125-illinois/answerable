package edu.illinois.cs.cs125.answerable.typeManagement

import edu.illinois.cs.cs125.answerable.*
import edu.illinois.cs.cs125.answerable.api.*
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

private open class BytesClassLoader(parentLoader: ClassLoader? = null) : ClassLoader(parentLoader ?: getSystemClassLoader()) {
    fun loadBytes(name: String, bytes: ByteArray): Class<*> {
        return defineClass(name, bytes, 0, bytes.size)
    }
}

private class DiamondClassLoader(primaryParent: ClassLoader,
                                 vararg val otherParents: ClassLoader) : BytesClassLoader(primaryParent) {
    override fun loadClass(name: String?): Class<*> {
        try {
            return super.loadClass(name)
        } catch (e: ClassNotFoundException) {
            otherParents.forEach {
                try {
                    return it.loadClass(name)
                } catch (ignored: ClassNotFoundException) { }
            }
        }
        throw ClassNotFoundException(name)
    }
}

/**
 * Creates a proxy to allow treating an object as an instance of a similarly-shaped class.
 * @param superClass the class that the object needs to appear as (be an instance of)
 * @param childClass the original class
 * @param forward an instance of childClass to which method calls will be forwarded
 * @param pool the type pool to look for classes in
 * @return an instance (the proxy) of a subclass of superClass
 */
internal fun mkProxy(superClass: Class<*>, childClass: Class<*>, forward: Any, pool: TypePool): Any {
    return mkProxy(superClass, superClass, childClass, childClass, forward, pool)
}

private data class TypeMap(
        val from: Class<*>,
        val to: Class<*>
)

/**
 * Determines what an inner class should be proxied to, if any.
 * @param outermostSuperClass the outer class a proxy is being made an instance of
 * @param childClass an inner class of the real/original class
 * @param pool type pool to look for classes in
 * @return a TypeMap that determines what classes to map fields and method between, or null if no proxy is needed
 */
private fun mostDerivedProxyableClass(outermostSuperClass: Class<*>, childClass: Class<*>?, pool: TypePool): TypeMap? {
    if (childClass == null) return null
    if (childClass.enclosingClass == null) return null
    val innerPath = childClass.name.split('$', limit = 2)[1]
    val correspondingSuper = "${outermostSuperClass.name}\$$innerPath"
    try {
        return TypeMap(to = pool.classForName(correspondingSuper), from = childClass)
    } catch (e: ClassNotFoundException) {
        return mostDerivedProxyableClass(outermostSuperClass, childClass.superclass, pool)
    }
}

private fun mkProxy(superClass: Class<*>, outermostSuperClass: Class<*>, childClass: Class<*>, outermostChildClass: Class<*>,
                    forward: Any, pool: TypePool): Any {
    if (superClass == childClass) return forward

    val subProxy = pool.getProxyInstantiator(superClass).newInstance()
    (subProxy as Proxy).setHandler { self, method, _, args ->
        childClass.getPublicFields().forEach { it.set(forward, self.javaClass.getField(it.name).get(self)) }
        val result = childClass.getMethod(method.name, *method.parameterTypes).invoke(forward, *args)
        childClass.getPublicFields().forEach { self.javaClass.getField(it.name).set(self, it.get(forward)) }
        if (result != null && result.javaClass.enclosingClass != null && result.javaClass.name.startsWith("${outermostChildClass.name}$")) {
            val innerMap = mostDerivedProxyableClass(outermostSuperClass, result.javaClass, pool)
            if (innerMap == null) {
                result
            } else {
                val innerProxy = mkProxy(innerMap.to, outermostSuperClass, innerMap.from, outermostChildClass, result, pool)
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
 * @param pool the type pool to get bytecode from
 * @return a mirror class suitable only for generation
 */
internal fun mkGeneratorMirrorClass(referenceClass: Class<*>, targetClass: Class<*>,
                                    pool: TypePool = TypePool(null), namePrefix: String = "m"): Class<*> {
    return mkGeneratorMirrorClass(referenceClass, referenceClass, targetClass,
            "answerablemirror.$namePrefix" + UUID.randomUUID().toString().replace("-", ""), mutableMapOf(), pool)
}

/**
 * Mirrors one class, which may be an inner class. (Recursive helper for mkGeneratorMirrorClass.)
 * @param baseClass the class to mirror
 * @param referenceClass the original, outermost reference class
 * @param targetClass the original, outermost submission class (to generate instances of)
 * @param mirrorName the desired fully-qualified dot name of the mirror class
 * @param mirrorsMade the map of names to finished mirrors
 * @param pool the type pool to get bytecode from
 * @return the mirrored class
 */
private fun mkGeneratorMirrorClass(baseClass: Class<*>, referenceClass: Class<*>, targetClass: Class<*>, mirrorName: String,
                                   mirrorsMade: MutableMap<String, Class<*>>, pool: TypePool): Class<*> {
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

    val classGen = ClassGen(pool.getBcelClassForClass(baseClass))

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
                val inner = pool.classForName(name.replace('/', '.'))
                val innerMirror = mkGeneratorMirrorClass(inner, referenceClass, targetClass, fixOuterClassName(name), mirrorsMade, pool)
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
    return pool.loadBytes(mirrorName, classGen.javaClass)
}

/**
 * Creates a mirror of an outer class with `final` members removed from classes and methods.
 * @param clazz an outer class
 * @param pool the type pool to get bytecode from and load classes into
 * @return a non-final version of the class with non-final members/classes
 */
internal fun mkOpenMirrorClass(clazz: Class<*>, pool: TypePool, namePrefix: String = "o"): Class<*> {
    return mkOpenMirrorClass(clazz, clazz, "answerablemirror.$namePrefix" + UUID.randomUUID().toString().replace("-", ""),
            mutableListOf(), pool)!!
}

private fun mkOpenMirrorClass(clazz: Class<*>, baseClass: Class<*>, newName: String, alreadyDone: MutableList<String>, pool: TypePool): Class<*>? {
    if (alreadyDone.contains(newName)) return null
    alreadyDone.add(newName)

    // Get a mutable ClassGen, initialized as a copy of the existing class
    val classGen = ClassGen(pool.getBcelClassForClass(clazz))
    val constantPoolGen = classGen.constantPool
    val constantPool = constantPoolGen.constantPool

    // Strip `final` off the class and its methods
    if (Modifier.isFinal(classGen.modifiers)) classGen.modifiers -= Modifier.FINAL
    classGen.methods.forEach { method ->
        if (Modifier.isFinal(method.modifiers)) method.modifiers -= Modifier.FINAL
    }

    // Recursively mirror inner classes
    val newBase = newName.split('$', limit = 2)[0]
    classGen.attributes.filterIsInstance(InnerClasses::class.java).firstOrNull()?.innerClasses?.forEach { innerClass ->
        val innerName = (constantPool.getConstant(innerClass.innerClassIndex) as? ConstantClass)?.getBytes(constantPool) ?: return@forEach
        if (innerName.startsWith(baseClass.slashName() + "$")) {
            if (Modifier.isFinal(innerClass.innerAccessFlags)) innerClass.innerAccessFlags -= Modifier.FINAL
            val innerPath = innerName.split('$', limit = 2)[1]
            mkOpenMirrorClass(pool.classForName(innerName.replace('/', '.')), baseClass,
                    "$newBase\$$innerPath", alreadyDone, pool)
        }
    }

    // Rename the class by changing all strings used by class or signature constants
    val newSlashBase = newBase.replace('.', '/')
    fun fixSignature(signature: String): String {
        return signature.replace("L${baseClass.slashName()};", "L$newSlashBase;")
                .replace("L${baseClass.slashName()}$", "L$newSlashBase$")
    }
    (1 until constantPool.length).forEach { idx ->
        val constant = constantPool.getConstant(idx)
        if (constant is ConstantClass) {
            val className = constant.getBytes(constantPool)
            val classNameParts = className.split('$', limit = 2)
            val newConstantValue = if (classNameParts[0] == baseClass.slashName()) {
                 if (classNameParts.size > 1) "$newSlashBase\$${classNameParts[1]}" else newSlashBase
            } else if (className.contains(';')) {
                fixSignature(className)
            } else {
                className
            }
            constantPoolGen.setConstant(constant.nameIndex, ConstantUtf8(newConstantValue))
        } else if (constant is ConstantNameAndType) {
            val signature = constant.getSignature(constantPool)
            constantPoolGen.setConstant(constant.signatureIndex, ConstantUtf8(fixSignature(signature)))
        }
    }
    classGen.methods.map { it.signatureIndex }.forEach { sigIdx ->
        val signature = (constantPool.getConstant(sigIdx) as ConstantUtf8).bytes
        constantPoolGen.setConstant(sigIdx, ConstantUtf8(fixSignature(signature)))
    }

    // Create and load the modified class
    // classGen.javaClass.dump("Opened${alreadyDone.indexOf(newName)}.class") // Uncomment for debugging
    return pool.loadBytes(newName, classGen.javaClass)
}

/**
 * Throws AnswerableBytecodeVerificationException if a mirror of the given generator class would fail with an illegal or absent member access.
 * @param referenceClass the original, non-mirrored reference class
 * @param pool the type pool to get bytecode from
 */
internal fun verifyMemberAccess(referenceClass: Class<*>, pool: TypePool = TypePool(null)) {
    verifyMemberAccess(referenceClass, referenceClass, mutableSetOf(), mapOf(), pool)
}

/**
 * Verifies the given class, which may be an inner class. (Recursive helper for the above overload.)
 * @param currentClass the class to verify
 * @param referenceClass the original, outermost reference class
 * @param checked the collection of classes already verified
 * @param dangerousAccessors members whose access will cause the given problem
 * @param pool the type pool to get bytecode from
 */
private fun verifyMemberAccess(currentClass: Class<*>, referenceClass: Class<*>, checked: MutableSet<Class<*>>,
                               dangerousAccessors: Map<String, AnswerableBytecodeVerificationException>, pool: TypePool) {
    if (checked.contains(currentClass)) return
    checked.add(currentClass)

    val toCheck = pool.getBcelClassForClass(currentClass)
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
                    verifyMemberAccess(pool.classForName(classConstant.getBytes(constantPool).replace('/', '.')), referenceClass, checked, dangersToInnerClasses, pool)
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
                        else -> throw IllegalStateException("Invalid type of AnswerableBytecodeVerificationException.member. Please report a bug.")
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

internal class TypePool(private val bytecodeProvider: BytecodeProvider?) {

    /*
     * For performance reasons, we want to re-use instantiators as much as possible.
     * A map is used for future-safety so that as many proxy instantiators as are needed can be created safely,
     * even if using only one is the most common use case.
     *
     * We map from 'superClass' instead of directly from 'proxyClass' as we won't have access to
     * the same reference to 'proxyClass' on future calls.
     */
    private val proxyInstantiators: MutableMap<Class<*>, ObjectInstantiator<out Any?>> = mutableMapOf()

    private var parent: TypePool? = null
    private var loader: BytesClassLoader = BytesClassLoader()
    private val bytecode = mutableMapOf<Class<*>, ByteArray>()

    constructor(bytecodeProvider: BytecodeProvider?, parentPool: TypePool?): this(bytecodeProvider) {
        parent = parentPool
        loader = BytesClassLoader(parent?.loader)
    }
    constructor(bytecodeProvider: BytecodeProvider?, commonLoader: ClassLoader) : this(bytecodeProvider) {
        loader = BytesClassLoader(commonLoader)
    }
    constructor(primaryParent: TypePool, vararg otherParents: TypePool) : this(null) {
        parent = primaryParent
        loader = DiamondClassLoader(primaryParent.loader, *otherParents.map { it.loader }.toTypedArray())
    }

    fun getBcelClassForClass(clazz: Class<*>): JavaClass {
        try {
            return parent!!.getBcelClassForClass(clazz)
        } catch (e: Exception) {
            // Ignored - parent couldn't find it
        }
        val bytecode = bytecode[clazz] ?: localGetBytecodeForClass(clazz).also { bytecode[clazz] = it }
        return ClassParser(bytecode.inputStream(), clazz.name).parse()
    }

    private fun localGetBytecodeForClass(clazz: Class<*>): ByteArray {
        return try {
            Repository.lookupClass(clazz).also { Repository.clearCache() }.bytes
        } catch (e: Exception) { // BCEL couldn't find it
            if (bytecodeProvider == null) throw NoClassDefFoundError("Could not find bytecode for $clazz, no BytecodeProvider specified")
            bytecodeProvider.getBytecode(clazz)
        }
    }

    fun loadBytes(name: String, bcelClass: JavaClass): Class<*> {
        val bytes = bcelClass.bytes
        return loader.loadBytes(name, bytes).also { bytecode[it] = bytes }
    }

    fun classForName(name: String): Class<*> {
        return Class.forName(name, true, loader)
    }

    fun getProxyInstantiator(superClass: Class<*>): ObjectInstantiator<out Any> {
        return proxyInstantiators[superClass] ?: run {
            val oldLoaderProvider = ProxyFactory.classLoaderProvider
            ProxyFactory.classLoaderProvider = ProxyFactory.ClassLoaderProvider { loader }

            val factory = ProxyFactory()

            factory.superclass = superClass
            factory.setFilter { it.name != "finalize" }
            val proxyClass = factory.createClass()

            // Restore the ClassLoaderProvider in case anything else in the process uses Javassist
            ProxyFactory.classLoaderProvider = oldLoaderProvider

            objenesis.getInstantiatorOf(proxyClass).also { proxyInstantiators[superClass] = it }
        }
    }

    fun asBytecodeProvider(): BytecodeProvider {
        return object : BytecodeProvider {
            override fun getBytecode(clazz: Class<*>): ByteArray {
                return bytecode[clazz]!!
            }
        }
    }

}
