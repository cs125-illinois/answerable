package edu.illinois.cs.cs125.answerable.api

import edu.illinois.cs.cs125.answerable.TestGenerator
import edu.illinois.cs.cs125.answerable.TestRunOutput
import edu.illinois.cs.cs125.answerable.getAttemptClass
import edu.illinois.cs.cs125.answerable.getSolutionClass
import edu.illinois.cs.cs125.answerable.runTestsUnsecured
import kotlin.random.Random

fun checkSubmission(
    submissionClass: String,
    solutionClass: String,
    solutionName: String = "",
    randomSeed: Long = Random.nextLong()
): TestRunOutput {
    return TestGenerator(getSolutionClass(solutionClass), solutionName)
        .loadSubmission(getAttemptClass(submissionClass))
        .runTestsUnsecured(randomSeed)
}