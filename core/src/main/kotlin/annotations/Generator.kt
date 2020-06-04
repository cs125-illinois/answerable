package edu.illinois.cs.cs125.answerable.annotations

import edu.illinois.cs.cs125.answerable.AnswerableMisuseException
import edu.illinois.cs.cs125.answerable.AnswerableVerificationException
import edu.illinois.cs.cs125.answerable.SourceLocation
import edu.illinois.cs.cs125.answerable.isStatic
import java.lang.reflect.Method

/**
 * Marks a method which can produce objects (or primitives) of arbitrary type for testing.
 *
 * The method must be static and take 2 parameters;
 * (1) an <tt>int</tt> representing the maximum complexity level of the output that should be produced, and
 * (2) a [java.util.Random] instance to be used if randomness is required.
 * The visibility and name do not matter. The method will be ignored in class design analysis, even if it is public.
 * If the method has the wrong signature, an [AnswerableMisuseException] will be thrown.
 *
 * Answerable will automatically detect the return type and override any existing generators for that type.
 * If the generator generates instance of the reference class, Answerable will automatically manage the transformation
 * required to use the method to generate instances of the submitted class. Due to this behavior, methods marked with
 * @[Generator] and whose return type is of the reference class <b>must</b> only use the <tt>public</tt> features
 * of the reference class, specifically those which the class design analysis pass will see. Answerable tries to
 * verify that your generators are safe and raise [AnswerableVerificationException]s if there is a problem.
 *
 * If a generator for the reference class is provided, and an @[Next] method is not provided, then the generator
 * will be used to generate new receiver objects on every iteration of the testing loop.
 *
 * @[Generator] annotations can have a [name] parameter, which must be unique.
 * If your class provides multiple generators for the same type, Answerable will resolve conflicts by choosing the one
 * whose [name] is in the [Solution.enabled] array on the @[Solution] annotation.
 *
 * If a helper method is needed that should not be included in class design analysis, see the @[Helper] annotation.
 * Generators can safely call each other and the @[Next] method (if any), even those which create instances
 * of the reference class.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Generator(
    val name: String = ""
) {
    companion object {
        private val parameterTypes = arrayOf(Int::class.java, java.util.Random::class.java)

        fun validate(klass: Class<*>) = klass.validateAnnotations(::validateMethod)

        fun validateMethod(m: Method) = m.ifHasAnnotation(Generator::class.java) { method ->
            val message = if (!method.isStatic()) {
                "@Generator methods must be static"
            } else if (!(method.parameterTypes contentEquals parameterTypes)) {
                "@Generator methods must take parameters (int, Random)"
            } else {
                null
            }
            if (message != null) {
                AnnotationError(
                    AnnotationError.Kind.Generator,
                    SourceLocation(method),
                    message
                )
            } else {
                null
            }
        }
    }
}

/**
 * Marks that a method parameter should use a particular generator.
 *
 * The selected generator does not need to be explicitly enabled under the @[Solution] annotation.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class UseGenerator(
    val name: String
)
