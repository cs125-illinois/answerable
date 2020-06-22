package edu.illinois.cs.cs125.answerable.annotations

import edu.illinois.cs.cs125.answerable.api.TestOutput
import java.lang.reflect.Method

/**
 * Annotation to mark a method as the reference solution for Answerable.
 *
 * If your class provides references to multiple different problems, you may annotate multiple methods
 * as a @[Solution]. You may provide a name using the [name] parameter on the @[Solution] annotation.
 * If you do not provide a name, the name of the method will be used. If a class contains multiple @[Solution]
 * annotations, their names must be unique within the class. However, if a class contains a single @[Solution]
 * method it will be loaded by default without needing to be named.
 *
 * If you provide multiple generators (or @[Next] methods) for the same type, then you should resolve conflicts
 * by naming the generators and including the name in the [enabled] string array parameter on the
 * @[Solution] annotation. The default name is the empty string.
 *
 * If the method under test prints to [System.out] or [System.err], specify the parameter [prints] as true.
 * Answerable's default verifier will assert that the printed strings to both are equal, and the [TestOutput.stdOut] and
 * [TestOutput.stdErr] fields of the [TestOutput] objects passed to @[Verify] will be non-null.
 *
 * @param name The name of this solution, useful when there are solutions to multiple questions in one class
 * @param enabled The names of the named annotations which are enabled for this @[Solution].
 * @param prints Whether or not this solution is expected to print to the console.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Solution(
    val name: String = DEFAULT_EMPTY_NAME,
    val enabled: Array<String> = [],
    val prints: Boolean = false
) {
    companion object {
        fun validate(context: ValidateContext) =
            validateNoDuplicates(context.referenceClass) +
                context.validateReferenceAnnotation(Solution::class.java, ::validateMethod)

        private fun validateNoDuplicates(klass: Class<*>): List<AnnotationError> =
            klass.methodsWithAnyAnnotation(Solution::class.java).let { methods ->
                methods.duplicateSolutionNames().let { names ->
                    if (names.isNotEmpty()) {
                        listOf(
                            AnnotationError(
                                AnnotationError.Kind.Solution,
                                SourceLocation(klass),
                                "Duplicate @Solution names: ${names.joinToString(separator = ", ")}"
                            )
                        )
                    } else {
                        klass.validateMembers(::validateMethod)
                    }
                }
            }

        private fun validateMethod(method: Method): AnnotationError? {
            val message = if (method.solutionName().isEmpty()) {
                "@Solution name should not be empty"
            } else {
                null
            }
            return if (message != null) {
                AnnotationError(
                    AnnotationError.Kind.Solution,
                    SourceLocation(method),
                    message
                )
            } else {
                null
            }
        }
    }
}

internal fun Class<*>.getSolution(solutionName: String = DEFAULT_EMPTY_NAME) =
    this.getNamedAnnotation(Solution::class.java, solutionName)
