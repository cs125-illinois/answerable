package edu.illinois.cs.cs125.answerable

import edu.illinois.cs.cs125.answerable.annotations.DEFAULT_EMPTY_NAME
import kotlin.random.Random

/**
 * Run Answerable on a submission and solution class that already exist in the testing sources.
 *
 * Never call this function to run untrusted code. Sandboxing tests belong in the jeedrunner tests.
 */
fun runAnswerableTest(
    submissionClass: String,
    solutionClass: String,
    solutionName: String = DEFAULT_EMPTY_NAME,
    randomSeed: Long = Random.nextLong()
): TestingResults {
    return TestGenerator(findClass(solutionClass), solutionName)
        .loadSubmission(findClass(submissionClass))
        .runTestsUnsecured(randomSeed)
}

/**
 * Convenience method for finding a (non-null) Class<*> from a class name.
 */
fun findClass(name: String): Class<*> = Class.forName(name)
