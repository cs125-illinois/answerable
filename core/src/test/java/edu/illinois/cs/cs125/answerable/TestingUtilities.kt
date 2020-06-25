package edu.illinois.cs.cs125.answerable

import edu.illinois.cs.cs125.answerable.annotations.DEFAULT_EMPTY_NAME
import edu.illinois.cs.cs125.answerable.classdesignanalysis.CDAConfig
import edu.illinois.cs.cs125.answerable.classdesignanalysis.CDAResult
import edu.illinois.cs.cs125.answerable.classdesignanalysis.classDesignAnalysis
import edu.illinois.cs.cs125.answerable.classdesignanalysis.defaultCDAConfig
import edu.illinois.cs.cs125.answerable.testing.TestingResults
import org.junit.jupiter.api.Assertions
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

internal fun assertClassDesignPasses(
    solution: Class<*>,
    submission: Class<*>,
    cdaConfig: CDAConfig = defaultCDAConfig
): CDAResult {
    val result = classDesignAnalysis(
        solution,
        submission,
        cdaConfig
    )
    result.errorMessages.forEach {
        Assertions.fail(it)
    }

    return result
}

internal fun assertClassDesignFails(
    solution: Class<*>,
    submission: Class<*>,
    config: CDAConfig = defaultCDAConfig
): CDAResult {
    val result = classDesignAnalysis(
        solution,
        submission,
        config
    )
    Assertions.assertFalse(result.allMatch)
    return result
}

/**
 * Convenience method for finding a (non-null) Class<*> from a class name.
 */
fun findClass(name: String): Class<*> = Class.forName(name)
