package edu.illinois.cs.cs125.answerable.annotations

import edu.illinois.cs.cs125.answerable.AnswerableMisuseException
import edu.illinois.cs.cs125.answerable.AnswerableVerificationException
import edu.illinois.cs.cs125.answerable.isStatic
import java.lang.reflect.Method

/**
 * Marks a method which can produce receiver objects for testing.
 *
 * The method must be static, return an instance of the reference class, and take 3 parameters;
 * (1) an instance of the reference class used as the receiver object in the last test,
 * which is null on the first test,
 * (2) an int representing the number of tests which have run, and
 * (3) a [java.util.Random] instance to be used if randomness is required.
 * The method visibility and name do not matter. The method will be ignored in class design analysis, even if it is
 * public. If the method has the wrong signature, an [AnswerableMisuseException] will be thrown when the reference is
 * loaded.
 *
 * The method will be called twice during each testing loop; once to create a receiver object for the reference
 * solution, and once to create a receiver object for the submitted class. Answerable will automatically manage
 * the transformation required to use the method to create instances of the submitted class. Due to this behavior,
 * methods marked with @[Next] <b>must</b> only use the <tt>public</tt> features of the reference class,
 * specifically those which the class design analysis pass will see. Answerable tries to verify that your
 * @[Next] methods are safe and raise [AnswerableVerificationException]s if there is a problem.
 *
 * If your class provides multiple @[Solution] methods which should use different @[Next] methods,
 * then the @[Next] annotations should each have a name parameter and should be explicitly enabled
 * via the <tt>enabled</tt> string array parameter of the appropriate @[Solution] annotations.
 *
 * If a helper method is needed that should not be included in class design analysis, see the @[Helper] annotation.
 * The @[Next] method <b>is</b> able to safely call methods marked with @[Generator], even a generator
 * for the reference class.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Next(
    val name: String = ""
) {
    companion object {
        private val parameterTypes = arrayOf(Int::class.java, java.util.Random::class.java)

        fun oldValidate(klass: Class<*>) = klass.validateMembers(::validateMethod)

        fun validateMethod(method: Method): AnnotationError? {
            val message = if (!method.isAnnotationPresent(Next::class.java)) {
                null
            } else if (!method.isStatic()) {
                "@Next methods must be static"
            } else if (!(method.parameterTypes.takeLast(2).toTypedArray() contentEquals parameterTypes)) {
                "@Next methods must take parameters (int, Random)"
                // TODO: this case is not right, regressed during refactor
            } else {
                null
            }
            return if (message != null) {
                AnnotationError(
                    AnnotationError.Kind.Next,
                    SourceLocation(method),
                    message
                )
            } else {
                null
            }
        }
    }
}
