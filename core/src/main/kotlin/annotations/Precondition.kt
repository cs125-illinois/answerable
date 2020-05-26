@file:Suppress("KDocUnresolvedReference")

package edu.illinois.cs.cs125.answerable.annotations

import edu.illinois.cs.cs125.answerable.AnswerableMisuseException
import edu.illinois.cs.cs125.answerable.JavaMode
import edu.illinois.cs.cs125.answerable.SourceLocation
import edu.illinois.cs.cs125.answerable.isStatic

/**
 * Marks a function as a precondition check on a set of arguments, allowing you to discard test cases which don't meet
 * a precondition.
 *
 * Preconditions should have a [name] corresponding to an @[Solution], otherwise they won't be used.
 *
 * A precondition method must take the same arguments as the @[Solution] annotation to which it corresponds.
 * @[Precondition] methods must be static if the corresponding @[Solution] is static. Precondition methods should
 * return a boolean, true if the precondition is satisfied and false otherwise. Answerable will inspect @[Precondition]
 * method signatures and throw an [AnswerableMisuseException] if any are incorrect.
 *
 * Preconditions will be only called on the arguments (and receiver) that will be supplied to the reference solution.
 * If a precondition fails, the test case will be "discarded." This is reflected in the [TestStep]. If too many
 * test cases are discarded, Answerable will give up. Answerable aims to complete (AKA not discard)
 * [TestRunnerArgs.numTests] tests.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Precondition(
    val name: String = DEFAULT_EMPTY_NAME
) {
    companion object {
        fun validate(klass: Class<*>): List<AnnotationError> {
            val methods = klass.declaredMethods

            val solutions = methods.filter { it.isAnnotationPresent(Solution::class.java) }
            val preconditions = methods.filter { it.isAnnotationPresent(Precondition::class.java) }

            preconditions.duplicateSolutionNames().also { names ->
                if (names.isNotEmpty()) {
                    return listOf(
                        AnnotationError(
                            AnnotationError.Kind.Precondition,
                            SourceLocation(klass),
                            "Duplicate @Precondition names: ${names.joinToString(separator = ", ")}"
                        )
                    )
                }
            }

            return preconditions.map { precondition ->
                if (precondition.returnType != Boolean::class.java) {
                    return@map Pair(precondition, "@Precondition methods must return a boolean.")
                }
                val solutionName = precondition.solutionName()

                if (klass.languageMode() == JavaMode) {
                    val solution = solutions.find {
                        it.solutionName() == solutionName
                    } ?: return@map Pair(precondition, "Can't find @Solution matching @Precondition name $solutionName")
                    if (solution.isStatic() && !precondition.isStatic()) {
                        return@map Pair(precondition, "@Precondition methods must be static if the solution is static.")
                    } else if (!(solution.genericParameterTypes contentEquals precondition.genericParameterTypes)) {
                        return@map Pair(
                            precondition,
                            "@Precondition methods must have the same parameter types as the corresponding @Solution."
                        )
                    }
                }
                Pair(precondition, null)
            }.mapNotNull { (method, message) ->
                if (message == null) {
                    null
                } else {
                    AnnotationError(AnnotationError.Kind.Precondition, SourceLocation(method), message)
                }
            }
        }
    }
}

internal fun Class<*>.getPrecondition(solutionName: String = DEFAULT_EMPTY_NAME) =
    this.getNamedAnnotation(Precondition::class.java, solutionName)
