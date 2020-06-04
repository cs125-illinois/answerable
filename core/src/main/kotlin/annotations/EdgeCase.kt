package edu.illinois.cs.cs125.answerable.annotations

import edu.illinois.cs.cs125.answerable.AnswerableVerificationException
import edu.illinois.cs.cs125.answerable.SourceLocation
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * Marks a field or function as storing or returning all the 'edge cases' for a type.
 *
 * Answerable provides default edge cases for primitive types, arrays of primitive types, and [String]s.
 * Generally speaking, these are all the cases returned by the default generator with complexity 0, and null if
 * applicable.
 *
 * User-provided edge cases will <i>override</i> the defaults; the defaults will be ignored. The cases will be
 * accessed only once, when testing begins. Cases should be provided in a non-empty array. Null arrays will be ignored.
 *
 * The cases for the type of the reference <b>must</b> be returned from a function, so that Answerable can
 * manage the transformation required to produce the same cases for the submission class. Due to this behavior,
 * cases should be created using only public features of the class, specifically those which the class design
 * analysis pass will see. Answerable tries to verify that these case functions are safe and raise
 * [AnswerableVerificationException]s if there is a problem.
 *
 * @param name The name of this annotation, which can be enabled by a [Solution] annotation.
 */

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class EdgeCase(
    val name: String = ""
) {
    companion object {
        fun validate(klass: Class<*>) = klass.validateAnnotations(::validateMethod, ::validateField)

        fun validateMethod(m: Method) = m.ifHasAnnotation(EdgeCase::class.java) { method ->
            val message = if (method.isAnnotationPresent(SimpleCase::class.java)) {
                "Can't use both @EdgeCase and @SimpleCase on the same method"
            } else {
                method.validateCase()?.let { error -> "@EdgeCase $error" }
            }
            if (message == null) {
                null
            } else {
                AnnotationError(
                    AnnotationError.Kind.EdgeCase,
                    SourceLocation(method),
                    message
                )
            }
        }

        fun validateField(f: Field) = f.ifHasAnnotation(EdgeCase::class.java) { field ->
            val message = if (field.isAnnotationPresent(SimpleCase::class.java)) {
                "Can't use both @EdgeCase and @SimpleCase on the same field"
            } else {
                field.validateCase()?.let { error -> "@EdgeCase $error" }
            }
            if (message == null) {
                null
            } else {
                AnnotationError(
                    AnnotationError.Kind.EdgeCase,
                    SourceLocation(field),
                    message
                )
            }
        }
    }
}
