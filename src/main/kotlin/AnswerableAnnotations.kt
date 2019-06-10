package edu.illinois.cs.cs125.answerable

// TODO: Once MisuseExceptions can be detected and thrown, update docs

/**
 * Annotation to mark a method as the reference solution for Answerable.
 *
 * If your class provides references to multiple different problems, supply a <tt>tag</tt> parameter to each
 * {@Code @Solution} annotation. You'll be able to invoke answerable with different test targets by specifying a tag.
 * Answerable's default tag is <tt>"solution"</tt>.
 *
 * If you provide multiple generators (or {@Code @Next} methods) for the same type, then you should resolve conflicts
 * by naming the generators and including the name in the <tt>generators</tt> string array parameter on the
 * {@Code @Solution} annotation. The default name is the empty string.
 *
 * If the method under test prints to System.out or System.err, specify the parameter <tt>prints</tt> as true.
 * Answerable's default verifier will assert that the printed strings to both are equal, and the <tt>stdOut</tt> and
 * <tt>stdErr</tt> fields of the <tt>TestOutput</tt> objects passed to {@Code @Verify} will be non-null.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Solution(
    val name: String = "",
    val generators: Array<String> = [],
    val prints: Boolean = false
)

/**
 * Define a timeout for the testing suite.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Timeout(
    val timeout: Long = 0
)

/**
 * Marks a method which can produce receiver objects for testing.
 * The method must be static, return an instance of the reference class, and take 3 parameters;
 * (1) an instance of the reference class used as the receiver object in the last test, which is null on the first test,
 * (2) an int representing the number of tests which have run, and
 * (3) a <tt>java.util.Random</tt> instance to be used if randomness is required.
 * The method visibility and name do not matter. The method will be ignored in class design analysis, even if it is public.
 * If the method has the wrong signature, an AnswerableMisuseException will be thrown.
 *
 * The method will be called twice during each testing loop; once to create a receiver object for the reference
 * solution, and once to create a receiver object for the submitted class. Answerable will automatically manage
 * the transformation required to use the method to create instances of the submitted class. Due to this behavior,
 * methods marked with {@Code @Next} <b>must</b> only use the <tt>public</tt> features of the reference class,
 * specifically those which the class design analysis pass will see. Answerable tries to verify that your
 * {@Code @Next} methods are safe and raise <tt>AnswerableVerificationException</tt>s if there is a problem.
 *
 * If your class provides multiple {@Code @Solution} methods which should use different {@Code @Next} methods,
 * then the {@Code @Next} annotations should each have a name parameter and should be explicitly enabled
 * via the <tt>generators</tt> string array parameter of the appropriate {@Code @Solution} annotations.
 *
 * If a helper method is needed that should not be included in class design analysis, see the {@Code @Helper} annotation.
 * The {@Code @Next} method <b>is</b> able to safely call methods marked with {@Code @Generator}, even a generator
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
 * (2) a <tt>java.util.Random</tt> instance to be used if randomness is required.
 * The visibility and name do not matter. The method will be ignored in class design analysis, even if it is public.
 * If the method has the wrong signature, an AnswerableMisuseException will be thrown.
 *
 * Answerable will automatically detect the return type and override any existing generators for that type.
 * If the generator generates instance of the reference class, Answerable will automatically manage the transformation
 * required to use the method to generate instances of the submitted class. Due to this behavior, methods marked with
 * {@Code @Generator} and whose return type is of the reference class <b>must</b> only use the <tt>public</tt> features
 * of the reference class, specifically those which the class design analysis pass will see. Answerable tries to
 * verify that your generators are safe and raise <tt>AnswerableVerificationException</tt>s if there is a problem.
 *
 * If a generator for the reference class is provided, and an {@Code @Next} method is not provided, then the generator
 * will be used to generate new receiver objects on every iteration of the testing loop.
 *
 * {@Code @Generator} annotations can have a <tt>name</tt> parameter, which must be unique.
 * If your class provides multiple generators for the same type, answerable will resolve conflicts by choosing the one
 * whose name is in the <tt>generators</tt> array on the {@Code @Solution} annotation.
 * The default tag if none is specified is the empty string.
 *
 * If a helper method is needed that should not be included in class design analysis, see the {@Code @Helper} annotation.
 * Generators can safely call each other and the {@Code @Next} method (if any), even if those which create instance
 * of the reference class.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Generator(
    val name: String = ""
)

/**
 * Marks a field or function as storing or returning all the 'edge cases' for a type. Answerable provides defaults.
 * User-provided edge cases will <i>override</i> the defaults; the defaults will be ignored. The cases will be
 * accessed only once, when testing begins. Cases should be provided in a non-empty array. Null arrays will be ignored.
 *
 * The cases for the type of the reference <b>must</b> be returned from a function, so that Answerable can
 * manage the transformation required to produce the same cases for the submission class. Due to this behavior,
 * cases should be created using only public features of the class, specifically those which the class design
 * analysis pass will see. Answerable tries to verify that these case functions are safe and raise
 * <tt>AnswerableVerificationException</tt>s if there is a problem.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class EdgeCase(
    val name: String = ""
)

/**
 * Marks a field or function as storing or returning all the 'corner cases' for a type. Answerable provides defaults.
 * User-provided edge cases will <i>override</i> the defaults; the defaults will be ignored. The cases will be
 * accessed only once, when testing begins. Cases should be provided in a non-empty array. Null arrays will be ignored.
 *
 * The cases for the type of the reference <b>must</b> be returned from a function, so that Answerable can
 * manage the transformation required to produce the same cases for the submission class. Due to this behavior,
 * cases should be created using only public features of the class, specifically those which the class design
 * analysis pass will see. Answerable tries to verify that these case functions are safe and raise
 * <tt>AnswerableVerificationException</tt>s if there is a problem.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class SimpleCase(
    val name: String = ""
)

/**
 * Marks a method as a custom verifier. The method will be called once per testing loop instead of Answerable's default verifier.
 * The method must be static and take 2 parameters;
 * (1) a <tt>TestOutput<></tt> instance representing the result of calling the reference method, and
 * (2) a <tt>TestOutput<></tt> instance representing the result of calling the submitted method.
 * The visibility, name, and return type do not matter. The method will be ignored in class design analysis, even if it is public.
 * If the method has the wrong signature, an AnswerableMisuseException will be thrown.
 * The <tt>TestOutput</tt> type should be parameterized with the type of the reference class, but this is optional.
 *
 * The <tt>receiver</tt> field in the <tt>TestOutput</tt> of the submitted method contains an instance of the submitted class
 * which can be used as though it were an instance of the reference class. Due to this behavior, <b>only</b> the <tt>public</tt>
 * members of the reference class should be accessed from this instance, specifically those which the class design
 * analysis pass will see. Answerable <i>does not</i> currently attempt to verify the safety of {@Code @Verify} methods.
 *
 * If verification fails, an {@Code @Verify} method should throw an exception, which will be caught by Answerable and recorded
 * in the testing output. JUnit assertions are satisfactory, but they are not the only option.
 *
 * If your class provides references to multiple problems, specify a <tt>tag</tt> parameter on each {@Code @Verify}
 * annotation. Answerable will use the verify method with the same tag as the {@Code @Solution} under test,
 * or the default verifier if none were provided. The default tag if none is specified is the empty string.
 *
 * Answerable's default verifier only compares the method return types for equality. This may be unsafe if the
 * method returns an instance of the declaring class. It will also not check the receiver objects for equality
 * after testing, for the same reason. If either of these is desired, you should provide a custom verifier.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Verify(
    val name: String = ""
)

/**
 * Marks a method as a helper method for {@Code @Next} or {@Code @Generator} methods which create instances of the
 * reference class. If a helper method were not tagged in this way, Answerable would try to call a method with the
 * same signature in the submitted class.
 *
 * Helper methods must be static.
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Helper

/**
 * Denotes that a method should be ignored by class design analysis.
 *
 * Methods which are annotated with {@Code @Ignore} are not safe to use in {@Code @Next} methods, or in {@Code @Generator}
 * methods which generate instances of the reference class.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Ignore
