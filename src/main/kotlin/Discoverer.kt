package edu.illinois.cs.cs125.answerable

import java.lang.IllegalStateException
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Type
import java.util.*

fun getSolutionClass(name: String): Class<*> = findClass(
    name,
    "Couldn't find reference solution."
)

fun getAttemptClass(name: String): Class<*> = findClass(
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

fun Class<*>.getReferenceSolutionMethod(): Method {
    val methods =
            this.declaredMethods
                .filter { method -> method.annotations.any { annotation ->
                    annotation.annotationClass == Solution::class
                } }
    if (methods.size != 1) throw IllegalStateException("Can't find singular solution method.")
    val solution = methods[0]
    solution.isAccessible = true
    return solution
}

fun Class<*>.findSolutionAttemptMethod(matchTo: Method): Method {
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
    return methods[0]
}

fun Class<*>.getPublicFields(): List<Field> =
    this.declaredFields.filter { Modifier.isPublic(it.modifiers) }

val ignoredAnnotations = setOf(Next::class, Generator::class, Verify::class, Helper::class, Ignore::class)

fun Class<*>.getPublicMethods(isReference: Boolean): List<Method> {
    val allPublicMethods = this.declaredMethods.filter { Modifier.isPublic(it.modifiers) }

    if (isReference) {
        return allPublicMethods.filter {
                method -> method.annotations.none { it.annotationClass in ignoredAnnotations }
        }
    }

    return allPublicMethods
}

fun Class<*>.getGenerators(): List<Method> =
        this.declaredMethods
            .filter { method -> method.isAnnotationPresent(Generator::class.java) }
            .map { it.isAccessible = true; it }

fun Class<*>.getAtNext(): Method? =
        this.declaredMethods.lastOrNull { method -> method.isAnnotationPresent(Next::class.java) }

fun Class<*>.getCustomVerifier(): Method? =
        this.declaredMethods.lastOrNull { method -> method.isAnnotationPresent(Verify::class.java) }

fun Method.isPrinter(): Boolean = this.getAnnotation(Solution::class.java)?.prints ?: false