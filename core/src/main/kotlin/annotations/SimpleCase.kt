package edu.illinois.cs.cs125.answerable.annotations

import edu.illinois.cs.cs125.answerable.AnswerableVerificationException
import java.lang.reflect.Field
import java.lang.reflect.Method

/**
 * Marks a field or function as storing or returning all the 'corner cases' for a type.
 *
 * Answerable provides default simple cases for primitive types, arrays of primitive types, and [String]s.
 * Generally speaking, these are all the cases returned by the default generator with complexity 1.
 *
 * User-provided simple cases will <i>override</i> the defaults; the defaults will be ignored. The cases will be
 * accessed only once, when testing begins. Cases should be provided in a non-empty array. Null arrays will be ignored.
 *
 * The cases for the type of the reference <b>must</b> be returned from a function, so that Answerable can
 * manage the transformation required to produce the same cases for the submission class. Due to this behavior,
 * cases should be created using only public features of the class, specifically those which the class design
 * analysis pass will see. Answerable tries to verify that these case functions are safe and raise
 * [AnswerableVerificationException]s if there is a problem.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class SimpleCase(
    val name: String = ""
) {
    companion object {
        fun validate(klass: Class<*>) = klass.validateMembers(::validateMethod, ::validateField)

        fun validateMethod(m: Method) = m.ifHasAnnotation(SimpleCase::class.java) { method ->
            val message = if (method.isAnnotationPresent(EdgeCase::class.java)) {
                "Can't use both @SimpleCase and @EdgeCase on the same method"
            } else {
                method.validateCase()?.let { error -> "@SimpleCase $error" }
            }
            if (message == null) {
                null
            } else {
                AnnotationError(
                    AnnotationError.Kind.SimpleCase,
                    SourceLocation(method),
                    message
                )
            }
        }

        fun validateField(f: Field) = f.ifHasAnnotation(SimpleCase::class.java) { field ->
            val message = if (field.isAnnotationPresent(EdgeCase::class.java)) {
                "Can't use both @SimpleCase and @EdgeCase on the same field"
            } else {
                field.validateCase()?.let { error -> "@SimpleCase $error" }
            }
            if (message == null) {
                null
            } else {
                AnnotationError(
                    AnnotationError.Kind.SimpleCase,
                    SourceLocation(field),
                    message
                )
            }
        }
    }
}
