package edu.illinois.cs.cs125.answerable.annotations

/**
 * Denotes that a (public) method should be ignored by class design analysis.
 *
 * Methods which are annotated with @[Ignore] are not safe to use in @[Next] methods, or in @[Generator]
 * methods which generate instances of the reference class.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Ignore
