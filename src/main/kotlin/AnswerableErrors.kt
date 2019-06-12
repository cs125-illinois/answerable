package edu.illinois.cs.cs125.answerable

/**
 * The type of exception raised when Answerable is unable to identify features of the submission class under test.
 */
class SubmissionMismatchException(msg: String) : Exception("\n$msg")

/**
 * The type of exception raised when Answerable detects an incorrect usage. Generally,
 * Answerable is able to detect these and raise exceptions during reference class load-time.
 */
class AnswerableMisuseException(msg: String) : Exception("\n$msg")

/**
 * The type of exception raised when Answerable detects an unsafe instruction in a static Answerable method.
 * Generally, Answerable is able to detect these and raise exceptions during reference class load-time.
 */
open class AnswerableVerificationException(msg: String) : Exception ("\n$msg")