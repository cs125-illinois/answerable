package edu.illinois.cs.cs125.answerable.annotations

import edu.illinois.cs.cs125.answerable.TestRunnerArgs
import java.lang.reflect.Method

/**
 * Specifies default arguments for the test execution of the @[Solution] or standalone @[Verify] problem method
 * to which it is applied.
 *
 * This is turned into a [TestRunnerArgs], with negative values treated as unspecified settings.
 * Any settings specified by this annotation are overridden by conflicting settings from [TestRunnerArgs] instances
 * specified in code.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Suppress("LongParameterList")
annotation class DefaultTestRunArguments(
    val numTests: Int = -1,
    val maxDiscards: Int = -1,
    val maxOnlyEdgeCaseTests: Int = -1,
    val maxOnlySimpleCaseTests: Int = -1,
    val numSimpleEdgeMixedTests: Int = -1,
    val numAllGeneratedTests: Int = -1,
    val numRegressionTests: Int = -1,
    val maxComplexity: Int = -1
) {
    companion object {
        fun validate(klass: Class<*>) = klass.validateMembers(::validateMethod)

        fun validateMethod(m: Method) = m.ifHasAnnotation(DefaultTestRunArguments::class.java) { method ->
            val message = if (method.isAnnotationPresent(Verify::class.java)) {
                if (!method.getAnnotation(Verify::class.java).standalone) {
                    "@DefaultTestRunArguments can only be applied to a standalone @Verify method"
                } else {
                    null
                }
            } else if (!method.isAnnotationPresent(Solution::class.java)) {
                "@DefaultTestRunArguments must be applied to a @Solution or standalone @Verify method"
            } else {
                null
            }
            if (message != null) {
                AnnotationError(
                    AnnotationError.Kind.DefaultTestRunArguments,
                    SourceLocation(method),
                    message
                )
            } else {
                null
            }
        }
    }
}
