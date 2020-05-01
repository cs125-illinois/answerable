package edu.illinois.cs.cs125.answerable.annotations

import edu.illinois.cs.cs125.answerable.SourceLocation
import java.lang.reflect.Method

/**
 * Define a timeout for the testing suite.
 *
 * A timeout of 0 means the test will never time out.
 *
 * @param timeout The timeout, in ms, that tests for this @Solution should use.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Timeout(
    val timeout: Long = Long.MIN_VALUE
) {
    companion object {
        fun validate(klass: Class<*>) = klass.declaredMethods.map { method -> validate(method) }.filterNotNull()

        fun validate(m: Method) = m.ifHasAnnotation(Timeout::class.java) { method ->
            val timeout = method.getAnnotation(Timeout::class.java).timeout
            val message = if (timeout == Long.MIN_VALUE) {
                "@Timeout annotation requires a timeout value"
            } else if (timeout < 0) {
                "@Timeout value cannot be negative"
            } else if (timeout == 0L) {
                "@Timeout value should not be zero, simply omit the @Timeout annotation"
            } else {
                null
            }
            if (message != null) {
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
