package edu.illinois.cs.cs125.answerable

import java.lang.IllegalStateException
import java.lang.reflect.Method
import java.lang.reflect.Type
import java.util.*

fun getSolutionClass(name: String): Class<*> = findClass(
    name,
    "Couldn't find reference solution."
)

fun getAttemptClass(name: String): Class<*> = findClass(
    name,
    "Couldn't find student attempt."
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

fun Class<*>.isClassDesignReference(): Boolean =
        this.annotations.any { it.annotationClass == Solution::class }

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
    val matchRType: Type = matchTo.genericReturnType
    val matchPTypes: Array<Type> = matchTo.genericParameterTypes

    var methods =
            this.declaredMethods
                .filter { it.name == matchName }
    if (methods.isEmpty()) {
        throw SubmissionMismatchError("Expected a method named `$matchName'.")
    }

    methods = methods.filter { it.genericReturnType == matchRType }
    if (methods.isEmpty()) {
        throw SubmissionMismatchError("Expected a method with return type `$matchRType'.")
    }

    methods = methods.filter { it.genericParameterTypes?.contentEquals(matchPTypes) ?: false }
    if (methods.isEmpty()) {
        throw SubmissionMismatchError(
            // TODO probably: improve this error message
            "Expected a method with parameter types `${Arrays.toString(matchPTypes)}'."
        )
    }
    // If the student code compiled, there can only be one method
    return methods[0]
}