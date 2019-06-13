package edu.illinois.cs.cs125.answerable

/**
 * Annotation to mark a method as the reference solution for Answerable.
 *
 * If your class provides references to multiple different problems, supply a <tt>tag</tt> parameter to each
 * {@Code @[Solution]} annotation. You'll be able to invoke answerable with different test targets by specifying a tag.
 *
 * If you provide multiple generators (or {@Code @[Next]} methods) for the same type, then you should resolve conflicts
 * by naming the generators and including the name in the <tt>enabled</tt> string array parameter on the
 * {@Code @[Solution]} annotation. The default name is the empty string.
 *
 * If the method under test prints to <tt>System.out</tt> or <tt>System.err</tt>, specify the parameter <tt>prints</tt> as true.
 * Answerable's default verifier will assert that the printed strings to both are equal, and the <tt>stdOut</tt> and
 * <tt>stdErr</tt> fields of the <tt>TestOutput</tt> objects passed to {@Code @Verify} will be non-null.
 *
 * @param name The name of this solution, useful when there are solutions to multiple questions in one class
 * @param enabled The names of the named annotations which are enabled for this {@Code @[Solution]}.
 * @param prints Whether or not this solution is expected to print to the console.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Solution(
    val name: String = "",
    val enabled: Array<String> = [],
    val prints: Boolean = false
)

/**
 * Define a timeout for the testing suite.
 *
 * A timeout of 0 means the test will never time out.
 *
 * @param timeout The timeout, in ms, that tests for this class should use.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Timeout(
    val timeout: Long = 0
)

/**
 * Marks a method which can produce receiver objects for testing.
 * The method must be static, return an instance of the reference class, and take 3 parameters;
 * (1) an instance of the reference class used as the receiver object in the last test, which is null on the first test,
 * (2) an int representing the number of tests which have run, and
 * (3) a [java.util.Random] instance to be used if randomness is required.
 * The method visibility and name do not matter. The method will be ignored in class design analysis, even if it is public.
 * If the method has the wrong signature, an [AnswerableMisuseException] will be thrown.
 *
 * The method will be called twice during each testing loop; once to create a receiver object for the reference
 * solution, and once to create a receiver object for the submitted class. Answerable will automatically manage
 * the transformation required to use the method to create instances of the submitted class. Due to this behavior,
 * methods marked with {@Code @[Next]} <b>must</b> only use the <tt>public</tt> features of the reference class,
 * specifically those which the class design analysis pass will see. Answerable tries to verify that your
 * {@Code @[Next]} methods are safe and raise [AnswerableVerificationException]s if there is a problem.
 *
 * If your class provides multiple {@Code @[Solution]} methods which should use different {@Code @[Next]} methods,
 * then the {@Code @[Next]} annotations should each have a name parameter and should be explicitly enabled
 * via the <tt>enabled</tt> string array parameter of the appropriate {@Code @[Solution]} annotations.
 *
 * If a helper method is needed that should not be included in class design analysis, see the {@Code @[Helper]} annotation.
 * The {@Code @[Next]} method <b>is</b> able to safely call methods marked with {@Code @[Generator]}, even a generator
 * for the reference class.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Next(
    val name: String = ""
)

/**
 * Marks a method which can produce objects (or primitives) of arbitrary type for testing.
 * The method must be static and take 2 parameters;
 * (1) an <tt>int</tt> representing the maximum complexity level of the output that should be produced, and
 * (2) a [java.util.Random] instance to be used if randomness is required.
 * The visibility and name do not matter. The method will be ignored in class design analysis, even if it is public.
 * If the method has the wrong signature, an [AnswerableMisuseException] will be thrown.
 *
 * Answerable will automatically detect the return type and override any existing generators for that type.
 * If the generator generates instance of the reference class, Answerable will automatically manage the transformation
 * required to use the method to generate instances of the submitted class. Due to this behavior, methods marked with
 * {@Code @[Generator]} and whose return type is of the reference class <b>must</b> only use the <tt>public</tt> features
 * of the reference class, specifically those which the class design analysis pass will see. Answerable tries to
 * verify that your generators are safe and raise [AnswerableVerificationException]s if there is a problem.
 *
 * If a generator for the reference class is provided, and an {@Code @[Next]} method is not provided, then the generator
 * will be used to generate new receiver objects on every iteration of the testing loop.
 *
 * {@Code @[Generator]} annotations can have a <tt>name</tt> parameter, which must be unique.
 * If your class provides multiple generators for the same type, answerable will resolve conflicts by choosing the one
 * whose name is in the <tt>enabled</tt> array on the {@Code @[Solution]} annotation.
 * The default tag if none is specified is the empty string.
 *
 * If a helper method is needed that should not be included in class design analysis, see the {@Code @[Helper]} annotation.
 * Generators can safely call each other and the {@Code @[Next]} method (if any), even if those which create instance
 * of the reference class.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Generator(
    val name: String = ""
)

/**
 * Marks a field or function as storing or returning all the 'edge cases' for a type.
 *
 * Answerable provides default edge cases for primitive types, arrays of primitive types, and [String]s.
 * Generally speaking, these are all the cases returned by the default generator with complexity 0, and null if applicable.
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
)

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
)

/**
 * Marks a method as a custom verifier. The method will be called once per testing loop instead of Answerable's default verifier.
 * The method must be static and take 2 parameters;
 * (1) a [TestOutput] instance representing the result of calling the reference method, and
 * (2) a [TestOutput] instance representing the result of calling the submitted method.
 * The visibility, name, and return type do not matter. The method will be ignored in class design analysis, even if it is public.
 * If the method has the wrong signature, an [AnswerableMisuseException] will be thrown.
 * The [TestOutput] type should be parameterized with the type of the reference class, but this is optional.
 *
 * The <tt>receiver</tt> field in the [TestOutput] of the submitted method contains an instance of the submitted class
 * which can be used as though it were an instance of the reference class. Due to this behavior, <b>only</b> the <tt>public</tt>
 * members of the reference class should be accessed from this instance, specifically those which the class design
 * analysis pass will see. Answerable <i>does not</i> currently attempt to verify the safety of {@Code @[Verify]} methods.
 *
 * If verification fails, an {@Code @[Verify]} method should throw an exception, which will be caught by Answerable and recorded
 * in the testing output. JUnit assertions are satisfactory, but they are not the only option.
 *
 * If your class provides references to multiple problems, specify a <tt>name</tt> parameter on each {@Code @[Verify]}
 * annotation. Answerable will use the verify method with the same name as the {@Code @[Solution]} under test,
 * or the default verifier if none were provided. The default tag if none is specified is the empty string.
 *
 * You can specify an entire test (the equivalent of an {@Code @[Solution]} annotation) by setting
 * 'standalone' to true. Answerable will produce two receiver objects and pass them to the {@Code @[Verify]} method,
 * wrapped in a [TestOutput] containing only the receiver.
 *
 * Answerable's default verifier only compares the method return types for equality. This may be unsafe if the
 * method returns an instance of the declaring class. It will also not check the receiver objects for equality
 * after testing, for the same reason. If either of these is desired, you should provide a custom verifier.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Verify(
    val name: String = "",
    val standalone: Boolean = false
)

/**
 * Marks a method as a helper method for {@Code @[Next]} or {@Code @[Generator]} methods which create instances of the
 * reference class. If a helper method were not tagged in this way, Answerable would try to call a method with the
 * same signature in the submitted class.
 *
 * Helper methods must be static.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Helper

/**
 * Denotes that a (public) method should be ignored by class design analysis.
 *
 * Methods which are annotated with {@Code @[Ignore]} are not safe to use in {@Code @[Next]} methods, or in {@Code @[Generator]}
 * methods which generate instances of the reference class.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Ignore
