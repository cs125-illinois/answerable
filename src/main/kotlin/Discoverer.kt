package edu.illinois.cs.cs125.answerable

import java.lang.IllegalStateException
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

internal fun getSolutionClass(name: String): Class<*> = findClass(
    name,
    "Couldn't find reference solution."
)

internal fun getAttemptClass(name: String): Class<*> = findClass(
    name,
    "Couldn't find student attempt class named $name."
)

private fun findClass(name: String, failMsg: String): Class<*> {
    val solutionClass: Class<*>
    try {
        solutionClass = Class.forName(name)
    } catch (unused: Exception) {
        throw IllegalStateException(failMsg)
    }
    return solutionClass
}

internal fun Class<*>.getReferenceSolutionMethod(name: String = ""): Method {
    val methods =
            this.declaredMethods
                .filter { it.getAnnotation(Solution::class.java)?.name?.equals(name) ?: false }

    if (methods.size != 1) throw IllegalStateException("Can't find singular solution method with tag `$name'.")
    val solution = methods[0]
    return solution.also { it.isAccessible = true }
}

internal fun Class<*>.findSolutionAttemptMethod(matchTo: Method): Method {
    val matchName: String = matchTo.name
    val matchRType: String = matchTo.genericReturnType.simpleName()
    val matchPTypes: List<String> = matchTo.genericParameterTypes.map { it.simpleName() }

    var methods =
            this.declaredMethods
                .filter { it.name == matchName }
    if (methods.isEmpty()) {
        throw SubmissionMismatchException("Expected a method named `$matchName'.")
    }

    methods = methods.filter { it.genericReturnType.simpleName() == matchRType }
    if (methods.isEmpty()) {
        throw SubmissionMismatchException("Expected a method with return type `$matchRType'.")
    }

    methods = methods.filter { it.genericParameterTypes?.map { it.simpleName() }?.equals(matchPTypes) ?: false }
    if (methods.isEmpty()) {
        throw SubmissionMismatchException(
            // TODO probably: improve this error message
            "Expected a method with parameter types `${matchPTypes.joinToString(prefix = "[", postfix = "]")}'."
        )
    }
    // If the student code compiled, there can only be one method
    return methods[0].also { it.isAccessible = true }
}

internal fun Class<*>.getPublicFields(): List<Field> =
    this.declaredFields.filter { Modifier.isPublic(it.modifiers) }

internal val ignoredAnnotations = setOf(Next::class, Generator::class, Verify::class, Helper::class, Ignore::class)

internal fun Class<*>.getPublicMethods(isReference: Boolean): List<Method> {
    val allPublicMethods = this.declaredMethods.filter { Modifier.isPublic(it.modifiers) }

    if (isReference) {
        return allPublicMethods.filter {
                method -> method.annotations.none { it.annotationClass in ignoredAnnotations }
        }
    }

    return allPublicMethods
}

internal fun Class<*>.getAllGenerators(): List<Method> =
        this.declaredMethods
            .filter { method -> method.isAnnotationPresent(Generator::class.java) }
            .map { it.isAccessible = true; it }

internal fun Class<*>.getEnabledGenerators(enabledNames: Array<String>): List<Method> =
        this.declaredMethods
            .filter { it.isAnnotationPresent(Generator::class.java) }
            .groupBy { it.returnType }
            .flatMap { entry -> when (entry.value.size) {
                1 -> entry.value
                else -> {
                    entry.value
                        .filter { it.getAnnotation(Generator::class.java).name in enabledNames }
                        .let { enabledGenerators ->
                            when (enabledGenerators.size) {
                                1 -> enabledGenerators
                                else ->
                                    throw AnswerableMisuseException("Failed to resolve @Generator conflict:\nMultiple enabled generators found for type `${entry.key.canonicalName}'.")
                            }
                        }
                }
            }}
            .map { it.isAccessible = true; it }

internal fun Class<*>.getDefaultAtNext(): Method? =
        this.declaredMethods
            .filter { it.getAnnotation(Next::class.java)?.name?.equals("") ?: false }
            .let { unnamedNexts -> when (unnamedNexts.size) {
                0 -> null
                1 -> unnamedNexts[0].also {it.isAccessible = true }
                else -> throw AnswerableMisuseException("Failed to resolve @Next conflict:\nFound multiple unnamed @Next annotations.")
            }}

internal fun Class<*>.getAtNext(enabledNames: Array<String>): Method? =
        this.declaredMethods
            .filter { it.getAnnotation(Next::class.java)?.name?.let { name -> enabledNames.contains(name) } ?: false }
            .let { atNexts -> when (atNexts.size) {
                0 -> null
                1 -> atNexts[0].also { it.isAccessible = true }
                else -> throw AnswerableMisuseException("Failed to resolve @Next conflict:\nFound multiple @Next annotations with enabled names.")
            }}
            ?: this.getDefaultAtNext()

internal fun Class<*>.getCustomVerifier(name: String): Method? =
        this.declaredMethods
            .filter { it.getAnnotation(Verify::class.java)?.name?.equals(name) ?: false }
            .let { verifiers -> when(verifiers.size) {
                0 -> null
                1 -> verifiers[0].also { it.isAccessible = true }
                else -> throw AnswerableMisuseException("Found multiple @Verify annotations with name `$name'.")
            }}

internal fun Method.isPrinter(): Boolean = this.getAnnotation(Solution::class.java)?.prints ?: false