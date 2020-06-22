package edu.illinois.cs.cs125.answerable.annotations

import java.lang.reflect.Method

/**
 * Define a timeout for the testing suite.
 *
 * Omit the annotation to prevent the test from timing out.
 *
 * @param timeout The timeout, in ms, that tests for this @Solution should use.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Timeout(
    val timeout: Long = Long.MIN_VALUE
) {
    companion object {
        fun validate(context: ValidateContext): List<AnnotationError> =
            context.validateAnnotation(Timeout::class.java, ::validateMethod)

        private fun validateMethod(method: Method): AnnotationError? {
            val timeout = method.getAnnotation(Timeout::class.java).timeout
            val isNonStandaloneVerify = !(method.getAnnotation(Verify::class.java)?.standalone ?: true)
            val message = when {
                !method.isAnnotationPresent(Solution::class.java) && isNonStandaloneVerify ->
                    "@Timeout can only be applied to testable (@Solution or standalone @Verify) methods"
                timeout == Long.MIN_VALUE -> "@Timeout annotation requires a timeout value"
                timeout < 0 -> "@Timeout value cannot be negative"
                timeout == 0L -> "@Timeout value should not be zero, instead, omit the @Timeout annotation"
                else -> null
            }
            return if (message != null) {
                AnnotationError(
                    AnnotationError.Kind.Timeout,
                    SourceLocation(method),
                    message
                )
            } else {
                null
            }
        }
    }
}

internal fun Method.getTimeout(): Long? = if (!isAnnotationPresent(Timeout::class.java)) {
    null
} else {
    getAnnotation(Timeout::class.java).timeout
}
