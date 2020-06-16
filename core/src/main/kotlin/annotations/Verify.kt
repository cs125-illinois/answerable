package edu.illinois.cs.cs125.answerable.annotations

import edu.illinois.cs.cs125.answerable.AnswerableMisuseException
import edu.illinois.cs.cs125.answerable.api.TestOutput
import edu.illinois.cs.cs125.answerable.isStatic
import java.util.Random

/**
 * Marks a method as a custom verifier. The method will be called once per testing loop instead of Answerable's default
 * verifier.
 *
 * The method must be static and take 2 parameters;
 * (1) a [TestOutput] instance representing the result of calling the reference method, and
 * (2) a [TestOutput] instance representing the result of calling the submitted method.
 *
 * Additionally, the method can optionally take a third parameter, a [java.util.Random] instance.
 *
 * The visibility, name, and return type do not matter. The method will be ignored in class design analysis, even if it
 * is public. If the method has the wrong signature, an [AnswerableMisuseException] will be thrown.
 * The [TestOutput] type should be parameterized with the type of the reference class, but this is optional.
 *
 * The [TestOutput.receiver] field in the [TestOutput] of the submitted method contains an instance of the submitted
 * class which can be used as though it were an instance of the reference class. However, you <em>cannot access
 * public static members of this instance.</em> Doing so will access the member of the reference class. Work around
 * by using a public <em>instance</em> getter method.
 *
 * Due to this behavior, <b>only</b> the <tt>public</tt>
 * members of the reference class should be accessed from this instance, specifically those which the class design
 * analysis pass will see. Answerable <i>does not</i> currently attempt to verify the safety of @[Verify] methods.
 *
 * If verification fails, an @[Verify] method should throw an exception, which will be caught by Answerable and
 * recorded in the testing output. JUnit assertions are satisfactory, but they are not the only option.
 *
 * If your class provides references to multiple problems, specify a [name] parameter on each @[Verify]
 * annotation. Answerable will use the verify method with the same [name] as the @[Solution] under test,
 * or the default verifier if none were provided. The default [name] if none is specified is the empty string.
 *
 * You can specify an entire test (the equivalent of an @[Solution] annotation) by setting
 * [standalone] to true. Answerable will produce two receiver objects and pass them to the @[Verify] method,
 * wrapped in a [TestOutput] containing only the receiver.
 *
 * Answerable's default verifier only compares the method return values (or thrown exceptions) for equality. This
 * may be unsafe if the method returns an instance of the declaring class. It will also not check the receiver objects
 * for equality after testing, for the same reason. If either of these is desired, you should provide a custom verifier.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Verify(
    val name: String = DEFAULT_EMPTY_NAME,
    val standalone: Boolean = false
) {
    companion object {
        private val parameterTypes = listOf(
            arrayOf(TestOutput::class.java, TestOutput::class.java),
            arrayOf(TestOutput::class.java, TestOutput::class.java, Random::class.java)
        )

        fun validate(klass: Class<*>): List<AnnotationError> {
            val methods = klass.declaredMethods

            val solutions = methods.filter { it.isAnnotationPresent(Solution::class.java) }
            val verifiers = methods.filter { it.isAnnotationPresent(Verify::class.java) }

            verifiers.duplicateSolutionNames().also { names ->
                if (names.isNotEmpty()) {
                    return listOf(
                        AnnotationError(
                            AnnotationError.Kind.Verify,
                            SourceLocation(klass),
                            "Duplicate @Verify names: ${names.joinToString(separator = ", ")}"
                        )
                    )
                }
            }

            return verifiers.map { verifier ->
                if (verifier.returnType != Void.TYPE) {
                    return@map Pair(
                        verifier,
                        "@Verify methods should be void (throw an exception if verification fails)"
                    )
                }

                if (!verifier.getAnnotation(Verify::class.java).standalone) {
                    val solutionName = verifier.solutionName()
                    solutions.find {
                        it.solutionName() == solutionName
                    } ?: return@map Pair(verifier, "Can't find @Solution matching @Verify name $solutionName")
                }

                if (!verifier.isStatic()) {
                    return@map Pair(verifier, "@Verify methods must be static")
                } else if (!parameterTypes.any { verifier.parameterTypes contentEquals it }) {
                    Pair(
                        verifier,
                        "@Verify methods must take parameters (TestOutput, TestOutput) and an optional java.util.Random"
                    )
                } else {
                    Pair(verifier, null)
                }
            }.mapNotNull { (method, message) ->
                if (message == null) {
                    null
                } else {
                    AnnotationError(
                        AnnotationError.Kind.Verify,
                        SourceLocation(method), message
                    )
                }
            }
        }
    }
}

internal fun Class<*>.getVerify(solutionName: String = DEFAULT_EMPTY_NAME) =
    this.getNamedAnnotation(Verify::class.java, solutionName)
