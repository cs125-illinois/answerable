package edu.illinois.cs.cs125.answerable


// TODO: Once MisuseExceptions can be detected and thrown, update docs

/**
 * Annotation to mark a method as the reference solution for Answerable.
 *
 * If multiple methods have this annotation, an AnswerableMisuseException will be triggered.
 * If you want to provide multiple reference methods in a single class (which may be useful, if
 * for example, you have an entire class of problems on a BinaryTree<T> class), then you should
 * provide an @Solution annotation on the method under test <i>for the current test run</i> and @Ignore the rest.
 *
 * If the method under test prints to System.out or System.err, specify the parameter '{@Code prints}' as true.
 * Answerable's default verifier will assert that the printed strings to both are equal, and the {@Code stdOut} and
 * {@Code stdErr} fields of the {@Code TestOutput} objects passed to {@Code @Verify} will be non-null.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Solution(
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
 * specifically those which the class design analysis pass will see.
 *
 * If a helper method is needed that should not be included in class design analysis, see the {@Code @Helper} annotation.
 * The {@Code @Next} method <b>is</b> able to safely call methods marked with {@Code @Generator}, even a generator
 * for the reference class.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Next

/**
 * Marks a method which can produce objects (or primitives) of arbitrary type for testing.
 * The method must be static and take 2 parameters;
 * (1) an <tt>int</tt> representing the maximum complexity level of the output that should be produced, and
 * (2) a <tt>java.util.Random</tt> instance to be used if randomness is required.
 * The visibility and name do not matter. The method will be ingored in class design analysis, even if it is public.
 * If the method has the wrong signature, an AnswerableMisuseException will be thrown.
 *
 * Answerable will automatically detect the return type and override any existing generators for that type.
 * If the generator generates instance of the reference class, Answerable will automatically manage the transformation
 * required to use the method to generate instances of the submitted class. Due to this behavior, methods marked with
 * {@Code @Generator} and whose return type is of the reference class <b>must</b> only use the <tt>public</tt> features
 * of the reference class, specifically those which the class design analysis pass will see.
 *
 * If a generator for the reference class is provided, and an {@Code @Next} method is not provided, then the generator
 * will be used to generate new receiver objects on every iteration of the testing loop.
 *
 * If a helper method is needed that should not be included in class design analysis, see the {@Code @Helper} annotation.
 * Generators can safely call each other and the {@Code @Next} method (if any), even if those which create instance
 * of the reference class.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Generator

/**
 * Marks a method as a custom verifier. The method will be called once per testing loop instead of Answerable's default verifier.
 * The method must be static and take 2 parameters;
 * (1) a <tt>TestOutput<></tt> instance representing the result of calling the reference method, and
 * (2) a <tt>TestOutput<></tt> instance representing the result of calling the submitted method.
 * The visibility, name, and return type do not matter. The method will be ignored in class design analysis, even if it is public.
 * If the method has the wrong signature, an AnswerableMisuseException will be thrown.
 * The <tt>TestOutput</tt> type should be parameterized with the type of the reference class, but this is optional.
 *
 * Answerable will automatically manage the transformation required to allow instances of the submitted class to be
 * treated as instances of the reference class. Due to this behavior, methods marked with {@Code @Verify} <b>must</b>
 * only use the <tt>public</tt> features of the reference class, specifically those which the class design analysis
 * pass will see.
 *
 * The <tt>receiver</tt> field in the <tt>TestOutput</tt> of the submitted method contains an instance of the submitted class
 * which can be used as though it were an instance of the reference class. Due to this behavior, <b>only</b> the <tt>public</tt>
 * members of the reference class should be accessed from this instance, specifically those which the class design
 * analysis pass will see.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Verify

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