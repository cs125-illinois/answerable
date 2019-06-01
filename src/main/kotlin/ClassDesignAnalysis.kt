package edu.illinois.cs.cs125.answerable

import java.lang.IllegalStateException
import java.lang.reflect.*
import java.util.*

class ClassDesignAnalysis(private val reference: Class<*>, private val attempt: Class<*>) {
    fun runSuite(
        name: Boolean = true,
        classStatus: Boolean = true,
        classModifiers: Boolean = true,
        typeParams: Boolean = true,
        superClasses: Boolean = true,
        fields: Boolean = true,
        methods: Boolean = true
    ) {
        if (name)           namesMatch()
        if (classStatus)    classStatusMatch()
        if (classModifiers) classModifiersMatch()
        if (typeParams)     typeParamsMatch()
        if (superClasses)   superClassesMatch()
        if (fields)         publicFieldsMatch()
        if (methods)        publicMethodsMatch()
    }

    fun namesMatch(): Boolean {
        if (reference.simpleName == attempt.simpleName)
            return true
        else throw ClassDesignMismatchException(
            "Expected a class named `${reference.simpleName}' but only found a class named `${attempt.simpleName}'."
        )
    }

    fun classStatusMatch(): Boolean {
        if (reference.isInterface == attempt.isInterface) {
            return true
        } else {
            val expected = if (reference.isInterface) "an interface" else "a class"
            val actual = if (attempt.isInterface) "an interface" else "a class"
            throw ClassDesignMismatchException("Expected $expected but found $actual.")
        }
    }

    fun classModifiersMatch(): Boolean {
        if (reference.modifiers == attempt.modifiers) {
            return true
        }

        val expected = Modifier.toString(reference.modifiers)
        val actual = Modifier.toString(attempt.modifiers)

        throw ClassDesignMismatchException(
            "Expected class modifiers : $expected\n" +
            "Found class modifiers    : $actual"
        )
    }

    fun typeParamsMatch(): Boolean {
        if (reference.typeParameters.map { it.name } == attempt.typeParameters.map { it.name }) {
            return true
        }

        val expected = reference.typeParameters
        val singleE = expected.size == 1
        val actual = attempt.typeParameters
        val singleA = actual.size == 1

        val buff = "   "
        val spaces =
            if (singleE == singleA) ("$buff ")
            else if (singleE && !singleA) buff
            else "$buff  "
        throw ClassDesignMismatchException(
            "Expected type parameter${if (singleE) "" else "s"} : ${expected.joinToString(prefix = "<", postfix = ">")}\n" +
            "Found type parameter${if (singleA) "" else "s"}$spaces: ${actual.joinToString(prefix = "<", postfix = ">")}"
        )
    }

    fun superClassesMatch(): Boolean {
        if (reference.genericSuperclass == attempt.genericSuperclass
            && reference.genericInterfaces contentEquals attempt.genericInterfaces
        )
            return true
        else throw ClassDesignMismatchException(
            mkSuperclassMismatchErrorMsg(
                reference.genericSuperclass, reference.genericInterfaces,
                attempt.genericSuperclass, attempt.genericInterfaces
            )
        )
    }

    fun publicFieldsMatch(): Boolean {
        fun mkFieldString(field: Field): String {
            val sb = StringBuilder()
            printModifiersIfNonzero(sb, field.modifiers, false)
            sb.append(field.type.simpleName).append(" ")
            sb.append(field.simpleName())
            return sb.toString()
        }

        val refFields =
            reference.getPublicFields()
                .map(::mkFieldString)
                .sorted()
        val attFields =
            attempt.getPublicFields()
                .map(::mkFieldString)
                .sorted()

        val looseRefFields = refFields.filter { it !in attFields }
        val looseAttFields = attFields.filter { it !in refFields }

        if (looseRefFields.isEmpty() && looseAttFields.isEmpty()) {
            return true
        }

        val msg: String = mkPublicApiMismatchMsg(looseRefFields, looseAttFields, "field")

        throw ClassDesignMismatchException(msg)
    }

    fun publicMethodsMatch(): Boolean {
        val refMethodData =
            (reference.getPublicMethods(true) + reference.constructors)
                .map { MethodData(it) }
                .sortedBy { it.signature }
        val attMethodData =
            (attempt.getPublicMethods(false) + attempt.constructors)
                .map { MethodData(it) }
                .sortedBy { it.signature }

        // First remove all methods that do match.
        val looseRefMethodData = refMethodData.filter { it !in attMethodData }
        val looseAttMethodData = attMethodData.filter { it !in refMethodData }
        // if everything matched, then we're done.
        if (looseRefMethodData.isEmpty() && looseAttMethodData.isEmpty()) {
            return true
        }

        // Otherwise, we're going to throw an exception eventually:
        val msg: String = mkPublicApiMismatchMsg(looseRefMethodData, looseAttMethodData, "method")

        throw ClassDesignMismatchException(msg)
    }
}

private fun mkSuperclassMismatchErrorMsg(
    referenceSC: Type?, referenceI: Array<Type>, attemptSC: Type?, attemptI: Array<Type>
): String {
    val scMatch = referenceSC == attemptSC
    val iMatch = referenceI contentEquals attemptI
    val refExtendsSomething = referenceSC != Object::class.java && referenceSC != null
    val attemptExtendsSomething = attemptSC != Object::class.java && attemptSC != null
    val refImplementsInterfaces = referenceI.isNotEmpty()
    val attemptImplementsInterfaces = attemptI.isNotEmpty()

    val msg: StringBuilder = StringBuilder("Expected class ")
    var also = false
    if (!scMatch) {
        if (refExtendsSomething) {
            msg.append("to extend `${referenceSC!!.typeName}', ") // refExtendsSomething => referenceSC != null
        } else {
            msg.append("to not extend any classes, ")
        }

        if (attemptExtendsSomething) {
            msg.append("but class extended `${attemptSC!!.typeName}'") // attemptExtendsSomething => attemptSC != null
        } else {
            msg.append("but class did not extend any classes")
        }
        also = true
    }
    if (!iMatch) {
        if (also) msg.append(";\nAlso expected class ")

        if (refImplementsInterfaces) {
            msg.append("to implement ${referenceI.joinToStringLast(last = ", and ") { "`${it.typeName}'" }}, ")
        } else {
            msg.append("to not implement any interfaces, ")
        }

        if (attemptImplementsInterfaces) {
            msg.append("but class implemented ${attemptI.joinToStringLast(last = ", and ") { "`${it.typeName}'" }}")
        } else {
            msg.append("but class did not implement any interfaces")
        }
    }
    msg.append(".")

    // If this function was called, then it must be the case that either !scMatch or !iMatch.
    // Therefore the bad message "Expected class."  can never occur.
    return msg.toString()
}

private fun <T> mkPublicApiMismatchMsg(exp: List<T>, act: List<T>, apiTypeName: String) =
    when (Pair(exp.isEmpty(), act.isEmpty())) {
        Pair(true, false) -> {
            val single = act.size == 1
                """
                    |Found ${if (single) "an " else ""}unexpected public $apiTypeName${if (single) "" else "s"}:
                    |${act.joinToString(separator = "\n  ", prefix = "  ")}
                """.trimMargin()
        }
        Pair(false, true) -> {
            val single = exp.size == 1
                """
                    |Expected ${if (single) "another" else "more"} public $apiTypeName${if (single) "" else "s"}:
                    |${exp.joinToString(separator = "\n  ", prefix = "  ")}
                """.trimMargin()
        }
        Pair(false, false) -> {
            // In this case the exact cause of the mismatch is provably undecidable
            // in the future we can try and provide better guesses here, but for now we'll dump everything
                """
                    |Expected your class to have public $apiTypeName${if (exp.size == 1) "" else "s"}:
                    |${exp.joinToString(separator = "\n  ", prefix = "  ")}
                    |but found public $apiTypeName${if (act.size == 1) "" else "s"}:
                    |${act.joinToString(separator = "\n  ", prefix = "  ")}
                """.trimMargin()
        }
        else -> throw IllegalStateException("Tried to generate API mismatch error, but no mismatch was found.\nPlease report a bug.")
    }

private fun <T> Array<out T>.joinToStringLast(
    separator: CharSequence = ", ",
    prefix: CharSequence = "",
    last: CharSequence = "",
    postfix: CharSequence = "",
    transform: ((T) -> CharSequence)? = null
): String {
    val safeTransform = transform ?: { it }

    if (this.size <= 1) {
        return this.joinToString(separator, prefix, postfix, transform = transform)
    }

    return (this.sliceArray(0 until this.size - 1)
        .joinToString(separator, prefix, postfix = "", transform = transform)
            + last + safeTransform(this.last()) + postfix)
}

class MethodData(
    val method: Executable
) {
    val name: String = method.name.split(".").last()
    val typeParams: Array<out TypeVariable<*>> = method.typeParameters
    val returnType: Type = when (method) {
        is Method -> method.genericReturnType
        is Constructor<*> -> method.declaringClass
        else -> throw IllegalStateException("This can't happen (non method/constructor in MethodData).")
    }
    val paramTypes: Array<out Type> = method.genericParameterTypes
    val exceptionTypes: Array<out Type> = method.genericExceptionTypes
    val isDefault: Boolean = when (method) {
        is Method -> method.isDefault
        is Constructor<*> -> false
        else -> false
    }

    val signature: String = toGenericString(method.modifiers, isDefault)

    // The below two methods are adapted out of openJDK's Executable.java file to Kotlin.
    // Note the change in creation of the name. We explicitly do *not* fully qualify it.
    private fun toGenericString(modifierMask: Int, isDefault: Boolean): String {
        try {
            val sb = StringBuilder()

            printModifiersIfNonzero(sb, modifierMask, isDefault)

            val typeparms = typeParams
            if (typeparms.isNotEmpty()) {
                sb.append(
                    typeparms.joinToString(
                        prefix = "<",
                        separator = ", ",
                        postfix = "> ",
                        transform = { it.simpleName() })
                )
            }

            if (method is Method) {
                sb.append(returnType.simpleName()).append(" ")
            }
            sb.append(name)

            sb.append('(')
            val sj = StringJoiner(", ")
            val params = paramTypes
            for (j in params.indices) {
                var param = params[j].simpleName()
                if (method.isVarArgs && j == params.size - 1)
                // replace T[] with T...
                    param = param.replaceFirst("\\[]$".toRegex(), "...")
                sj.add(param)
            }
            sb.append(sj.toString())
            sb.append(')')

            val exceptionTypes = exceptionTypes
            if (exceptionTypes.isNotEmpty()) {
                sb.append(
                    exceptionTypes.joinToString(
                        separator = ", ",
                        prefix = " throws ",
                        transform = { it.simpleName() })
                )
            }
            return sb.toString()
        } catch (e: Exception) {
            return "<$e>"
        }
    }

    override fun toString(): String = signature

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MethodData

        if (signature != other.signature) return false

        return true
    }

    override fun hashCode(): Int {
        return signature.hashCode()
    }
}

private fun printModifiersIfNonzero(sb: StringBuilder, mask: Int, isDefault: Boolean) {
    var mod = mask

    if (mod != 0 && !isDefault) {
        sb.append(Modifier.toString(mod)).append(' ')
    } else {
        val accessModifiers = Modifier.PUBLIC or Modifier.PROTECTED or Modifier.PRIVATE
        val accessMod = mod and (accessModifiers)
        if (accessMod != 0)
            sb.append(Modifier.toString(accessMod)).append(' ')
        if (isDefault)
            sb.append("default ")
        mod = mod and accessModifiers.inv()
        if (mod != 0)
            sb.append(Modifier.toString(mod)).append(' ')
    }
}

fun Field.simpleName(): String = this.name.split(".").last()

fun Type.simpleName() = this.typeName.split(".").last()