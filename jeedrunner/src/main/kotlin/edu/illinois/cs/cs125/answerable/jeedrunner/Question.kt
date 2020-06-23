@file:Suppress("NEWER_VERSION_IN_SINCE_KOTLIN", "unused")

package edu.illinois.cs.cs125.answerable.jeedrunner

import edu.illinois.cs.cs125.answerable.TestEnvironment
import edu.illinois.cs.cs125.answerable.TestGenerator
import edu.illinois.cs.cs125.answerable.TestRunner
import edu.illinois.cs.cs125.answerable.testing.TestRunnerArgs
import edu.illinois.cs.cs125.answerable.testing.TestingResults
import edu.illinois.cs.cs125.answerable.testing.defaultArgs
import edu.illinois.cs.cs125.jeed.core.CompiledSource
import edu.illinois.cs.cs125.jeed.core.Sandbox
import kotlin.random.Random

internal data class Question(
    val testGenerator: TestGenerator,
    val language: QuestionLanguage?,
    val commonSource: CompiledSource?,
    val classLoaderConfiguration: Sandbox.ClassLoaderConfiguration,
    val executionConfiguration: Sandbox.ExecutionArguments
) {
    fun createJeedEnvironment(): TestEnvironment {
        return TestEnvironment(
            jeedOutputCapturer,
            jeedSandbox(
                loaderConfig = classLoaderConfiguration,
                executeConfig = executionConfiguration
            )
        )
    }
}

enum class QuestionLanguage(val extension: String) {
    JAVA("java"),
    KOTLIN("kt")
}

/**
 * Allows running tests on a submission, multiple times or with a specified seed if desired.
 * The Jeed-runner-specific version of [TestRunner].
 */
class JeedTestRunner internal constructor(
    private val answerableRunner: TestRunner,
    private val environment: TestEnvironment
) {

    /**
     * Tests the submission in the Jeed sandbox.
     *
     * @param seed the seed to use for random generation (defaults to random)
     * @param testRunnerArgs any additional overrides of test run arguments
     * @return the test results
     */
    @JvmOverloads
    fun runTests(
        seed: Long = Random.nextLong(),
        testRunnerArgs: TestRunnerArgs = defaultArgs
    ): TestingResults {
        return answerableRunner.runTests(seed, environment, testRunnerArgs)
    }

    @SinceKotlin(JAVA_ONLY)
    fun runTests(testRunnerArgs: TestRunnerArgs): TestingResults {
        return runTests(Random.nextLong(), testRunnerArgs)
    }
}
