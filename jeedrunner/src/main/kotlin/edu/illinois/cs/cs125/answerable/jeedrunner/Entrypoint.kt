package edu.illinois.cs.cs125.answerable.jeedrunner

import edu.illinois.cs.cs125.answerable.TestEnvironment
import edu.illinois.cs.cs125.answerable.TestGenerator
import edu.illinois.cs.cs125.answerable.TestRunnerArgs
import edu.illinois.cs.cs125.answerable.TestingResults
import edu.illinois.cs.cs125.answerable.annotations.DEFAULT_EMPTY_NAME
import edu.illinois.cs.cs125.jeed.core.CompilationArguments
import edu.illinois.cs.cs125.jeed.core.Source
import edu.illinois.cs.cs125.jeed.core.compile
import kotlin.random.Random.Default.nextLong

/**
 * Simple entry point for securely running tests on Java classes.
 * @param reference reference solution class code
 * @param submission student submission class code
 * @param common common code or null if none
 * @param className the name of the class from [reference] and [submission] under test
 * @param solutionName which question to use from [reference]
 * @return the results of the testing
 */
@Suppress("LongParameterList")
fun testFromStrings(
    reference: String,
    submission: String,
    common: String? = null,
    className: String,
    solutionName: String = DEFAULT_EMPTY_NAME,
    testRunnerArgs: TestRunnerArgs = TestRunnerArgs()
): TestingResults {

    val referenceSource = Source(mapOf("Reference.java" to reference))
    val submissionSource = Source(mapOf("Submission.java" to submission))

    val commonSource: Source? = common?.run { Source(mapOf("Common.java" to common)) }

    val parentClassLoader = TestingClassLoader(className)

    val commonCompiledSource = commonSource?.compile(CompilationArguments(parentClassLoader = parentClassLoader))
    val parentClassLoaderIsNow = commonCompiledSource?.classLoader ?: parentClassLoader
    val referenceClassLoader = referenceSource
        .compile(
            CompilationArguments(
                parentClassLoader = parentClassLoaderIsNow, parentFileManager = commonCompiledSource?.fileManager
            )
        ).classLoader
    val submissionClassLoader = submissionSource
        .compile(
            CompilationArguments(
                parentClassLoader = parentClassLoaderIsNow, parentFileManager = commonCompiledSource?.fileManager
            )
        ).classLoader

    return TestGenerator(
        referenceClassLoader.loadClass(className),
        bytecodeProvider = answerableBytecodeProvider(referenceClassLoader),
        solutionName = solutionName
    ).loadSubmission(
        submissionClassLoader.loadClass(className),
        bytecodeProvider = answerableBytecodeProvider(submissionClassLoader)
    ).runTests(nextLong(), TestEnvironment(jeedOutputCapturer, jeedSandbox()), testRunnerArgs)
}

class TestingClassLoader(private val klassName: String) : ClassLoader() {
    // Invert the usual delegation strategy for classes in this package to avoid using the system ClassLoader
    override fun loadClass(name: String): Class<*> {
        return if (name == klassName) {
            throw ClassNotFoundException()
        } else {
            super.loadClass(name)
        }
    }

    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        return if (name == klassName) {
            throw ClassNotFoundException()
        } else {
            super.loadClass(name, resolve)
        }
    }
}

