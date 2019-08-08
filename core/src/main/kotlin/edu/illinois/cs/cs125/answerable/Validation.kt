package edu.illinois.cs.cs125.answerable

import edu.illinois.cs.cs125.answerable.api.*
import edu.illinois.cs.cs125.answerable.typeManagement.sourceName
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.*

sealed class ValidationResult<E, T> {
    open val errors: List<E>? = null
    open val value: T? = null
}
class Ok<E, T>(override val value: T) : ValidationResult<E, T>()
class Nonfatal<E, T>(override val errors: List<E>, override val value: T) : ValidationResult<E, T>()
class Fatal<E, T>(override val errors: List<E>) : ValidationResult<E, T>()

infix fun <E, T, R> ValidationResult<E, T>.then(f: (T) -> ValidationResult<E, R>): ValidationResult<E, R> = when (this) {
    is Ok -> f(this.value)
    is Nonfatal -> f(this.value).let { other -> when (other) {
        is Ok -> Nonfatal(this.errors, other.value)
        is Nonfatal -> Nonfatal(this.errors + other.errors, other.value)
        is Fatal -> Fatal<E, R>(this.errors + other.errors)
    } }
    is Fatal -> Fatal(this.errors)
}

fun <E, T> succeed(v: T): ValidationResult<E, T> = Ok(v)
fun <E, T> complain(e: E, v: T): ValidationResult<E, T> = Nonfatal(listOf(e), v)
fun <E, T> explode(e: E): ValidationResult<E, T> = Fatal(listOf(e))
fun <E, T> fromErrors(es: List<E>, value: T): ValidationResult<E, T> =
    if (es.isEmpty()) Ok(value) else Nonfatal(es, value)

internal fun validateStaticSignatures(referenceClass: Class<*>) {
    val allMethods = referenceClass.declaredMethods

    val result =  validateGenerators(allMethods) then
            { validateNexts(referenceClass, allMethods) } then
            { validateVerifiers(allMethods) } then
            { validatePreconditions(allMethods) } then
            { validateCaseMethods(allMethods) } then
            { validateCaseFields(referenceClass, referenceClass.declaredFields) } then
            { validateRunArgs(allMethods) }

    when (result) {
        is Ok -> return
        is Nonfatal -> throw AnswerableMisuseException(result.errors.joinToString("\n\n"))
        is Fatal -> throw AnswerableMisuseException(result.errors.joinToString("\n\n"))
    }
}

private val generatorPTypes = arrayOf(Int::class.java, java.util.Random::class.java)
private fun validateGenerators(methods: Array<Method>): ValidationResult<String, Unit> {
    val generators = methods.filter { it.isAnnotationPresent(Generator::class.java) }
    val errors: MutableList<String> = mutableListOf()

    generators.forEach { method ->
        if (!Modifier.isStatic(method.modifiers)) {
            errors.add("""
                @Generator methods must be static.
                While validating generator method `${MethodData(method)}'.
            """.trimIndent())
        }

        if (!(method.parameterTypes contentEquals generatorPTypes)) {
            errors.add("""
                @Generator methods must take parameter types [int, Random].
                While validating @Generator method `${MethodData(method)}'.
            """.trimIndent())
        }
    }

    return fromErrors(errors, Unit)
}

private fun validateNexts(clazz: Class<*>, methods: Array<Method>): ValidationResult<String, Unit> {
    val nexts = methods.filter { it.isAnnotationPresent(Next::class.java) }
    val errors: MutableList<String> = mutableListOf()

    nexts.forEach { method ->
        if (!Modifier.isStatic(method.modifiers)) {
            errors.add("""
                @Next methods must be static.
                While validating @Next method `${MethodData(method)}'.
            """.trimIndent())
        }

        if (!(method.parameterTypes contentEquals arrayOf(clazz, Int::class.java, java.util.Random::class.java))) {
            errors.add("""
                @Next method must take parameter types [${clazz.sourceName}, int, Random].
                While validating @Next method `${MethodData(method)}'.
            """.trimIndent())
        }
    }

    return fromErrors(errors, Unit)
}

private val validatePTypes1 = arrayOf(TestOutput::class.java, TestOutput::class.java)
private val validatePTypes2 = arrayOf(TestOutput::class.java, TestOutput::class.java, Random::class.java)
private fun validateVerifiers(methods: Array<Method>): ValidationResult<String, Unit> {
    val verifiers = methods.filter { it.isAnnotationPresent(Verify::class.java) }
    val errors: MutableList<String> = mutableListOf()

    verifiers.forEach { method ->
        if (!Modifier.isStatic(method.modifiers)) {
            errors.add("""
                @Verify methods must be static.
                While validating @Verify method `${MethodData(method)}'.
            """.trimIndent())
        }

        if (!((method.parameterTypes contentEquals validatePTypes1) || (method.parameterTypes contentEquals validatePTypes2))) {
            errors.add("""
                @Verify methods must take parameter types [TestOutput, TestOutput] and optionally a java.util.Random.
                While validating @Verify method `${MethodData(method)}'.
            """.trimIndent())
        }

        if (method.returnType != Void.TYPE) {
            errors.add("""
                @Verify methods should be void. Throw an exception if verification fails.
                While validating @Verify method `${MethodData(method)}'.
            """.trimIndent())
        }
    }

    return fromErrors(errors, Unit)
}

private fun validatePreconditions(methods: Array<Method>): ValidationResult<String, Unit> {
    val preconditions = methods.filter { it.isAnnotationPresent(Precondition::class.java) }
    val errors: MutableList<String> = mutableListOf()

    preconditions.forEach { method ->
        if (method.returnType != Boolean::class.java) {
            throw AnswerableMisuseException("""
                @Precondition methods must return a boolean.
                While validating @Precondition method `${MethodData(method)}'.
            """.trimIndent())
        }

        val solution = methods.find {
            it.getAnnotation(Solution::class.java)?.name?.equals(method.getAnnotation(Precondition::class.java)?.name)
                ?: false } ?: return@forEach // nothing to compare to

        if (Modifier.isStatic(solution.modifiers) && !Modifier.isStatic(method.modifiers)) {
            errors.add("""
                @Precondition methods must be static if the corresponding @Solution is static.
                While validating @Precondition method `${MethodData(method)}'.
            """.trimIndent())
        }

        if (!solution.genericParameterTypes!!.contentEquals(method.genericParameterTypes)) {
            errors.add("""
                @Precondition methods must have the same parameter types as the corresponding @Solution.
                While validating @Precondition method `${MethodData(method)}'.
            """.trimIndent())
        }
    }

    return fromErrors(errors, Unit)
}

private val caseAnnotations = setOf(EdgeCase::class.java, SimpleCase::class.java)
private fun validateCaseMethods(methods: Array<Method>): ValidationResult<String, Unit> {
    val cases = methods.filter { method -> caseAnnotations.any { method.isAnnotationPresent(it) } }
    val errors: MutableList<String> = mutableListOf()

    cases.forEach { method ->
        val caseString = if (method.isAnnotationPresent(EdgeCase::class.java)) "@EdgeCase" else "@SimpleCase"

        if (!Modifier.isStatic(method.modifiers)) {
            errors.add("""
                $caseString methods must be static.
                While validating $caseString method `${MethodData(method)}'.
            """.trimIndent())
        }

        if (method.parameterTypes.isNotEmpty()) {
            errors.add("""
                $caseString methods must not take any parameters.
                While validating $caseString method `${MethodData(method)}'.
            """.trimIndent())
        }

        if (!method.returnType.isArray) {
            errors.add("""
                $caseString methods must return an array.
                While validating $caseString method `${MethodData(method)}'.
            """.trimIndent())
        }
    }

    return fromErrors(errors, Unit)
}

private fun validateCaseFields(clazz: Class<*>, fields: Array<Field>): ValidationResult<String, Unit> {
    val cases = fields.filter { field -> caseAnnotations.any { field.isAnnotationPresent(it) } }
    val errors: MutableList<String> = mutableListOf()

    cases.forEach { field ->
        val caseString = if (field.isAnnotationPresent(EdgeCase::class.java)) "@EdgeCase" else "@SimpleCase"

        if (!Modifier.isStatic(field.modifiers)) {
            errors.add("""
                $caseString fields must be static.
                While validating $caseString field `$field'.
            """.trimIndent())
        }

        if (!field.type.isArray) {
            errors.add("""
                $caseString fields must store an array.
                While validating $caseString field `$field'.
            """.trimIndent())
        }

        if (field.type == clazz) {
            errors.add("""
                $caseString cases for the reference class must be represented by a function.
                While validating $caseString field `$field'.
            """.trimIndent())
        }
    }

    return fromErrors(errors, Unit)
}

private fun validateRunArgs(methods: Array<Method>): ValidationResult<String, Unit> {
    val errors: MutableList<String> = mutableListOf()

    methods.filter { it.isAnnotationPresent(DefaultTestRunArguments::class.java) }.forEach {
        val message = """
                @DefaultTestRunArguments can only be applied to a @Solution or standalone @Verify method.
                While validating method `${MethodData(it)}'.
            """.trimIndent()
        if (it.isAnnotationPresent(Verify::class.java)) {
            val validateAnnotation = it.getAnnotation(Verify::class.java)
            if (!validateAnnotation.standalone) errors.add(message)
        } else if (!it.isAnnotationPresent(Solution::class.java)) {
            errors.add(message)
        }
    }

    return fromErrors(errors, Unit)
}