@file:Suppress("NEWER_VERSION_IN_SINCE_KOTLIN")

package edu.illinois.cs.cs125.answerable.jeedrunner

import edu.illinois.cs.cs125.answerable.TestEnvironment
import edu.illinois.cs.cs125.answerable.TestGenerator
import edu.illinois.cs.cs125.answerable.TestRunner
import edu.illinois.cs.cs125.answerable.TestRunnerArgs
import edu.illinois.cs.cs125.answerable.TestingResults
import edu.illinois.cs.cs125.answerable.defaultArgs
import edu.illinois.cs.cs125.answerable.jeedOutputCapturer
import edu.illinois.cs.cs125.answerable.jeedSandbox
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
        return TestEnvironment(jeedOutputCapturer, jeedSandbox(
            loaderConfig = classLoaderConfiguration,
            executeConfig = executionConfiguration
        ))
    }
}

enum class QuestionLanguage(val extension: String) {
    JAVA("java"),
    KOTLIN("kt")
}

class JeedTestRunner internal constructor(
    private val answerableRunner: TestRunner,
    private val environment: TestEnvironment
) {

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
