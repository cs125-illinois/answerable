package edu.illinois.cs.cs125.answerable

import kotlin.random.Random

/**
 * Run Answerable on a submission and solution class that already exist in the testing sources.
 *
 * Never call this function to run untrusted code. Sandboxing tests belong in the jeedrunner tests.
 */
fun runAnswerableTest(
    submissionClass: String,
    solutionClass: String,
    solutionName: String = "",
    randomSeed: Long = Random.nextLong()
): TestingResults {
    return TestGenerator(getSolutionClass(solutionClass), solutionName)
        .loadSubmission(getAttemptClass(submissionClass))
        .runTestsUnsecured(randomSeed)
}
