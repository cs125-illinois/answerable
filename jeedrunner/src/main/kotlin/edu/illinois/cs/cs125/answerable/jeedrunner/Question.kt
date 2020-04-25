package edu.illinois.cs.cs125.answerable.jeedrunner

import edu.illinois.cs.cs125.answerable.TestEnvironment
import edu.illinois.cs.cs125.answerable.TestGenerator
import edu.illinois.cs.cs125.answerable.jeedOutputCapturer
import edu.illinois.cs.cs125.answerable.jeedSandbox
import edu.illinois.cs.cs125.jeed.core.CompiledSource
import edu.illinois.cs.cs125.jeed.core.Sandbox

data class Question(
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
