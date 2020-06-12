package edu.illinois.cs.cs125.answerable.jeedrunner

import edu.illinois.cs.cs125.answerable.TestEnvironment
import edu.illinois.cs.cs125.answerable.TestGenerator
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
fun testFromStrings(
    reference: String,
    submission: String,
    common: String? = null,
    className: String,
    solutionName: String = DEFAULT_EMPTY_NAME
): TestingResults {

    val referenceSource = Source(mapOf("Reference.java" to reference))
    val submissionSource = Source(mapOf("Submission.java" to submission))

    val commonSource: Source? = common?.run { Source(mapOf("Common.java" to common)) }

    val comCL = commonSource?.compile()
    val refCL = referenceSource
        .compile(
            CompilationArguments(
                parentClassLoader = comCL?.classLoader, parentFileManager = comCL?.fileManager
            )
        ).classLoader
    val subCL = submissionSource
        .compile(
            CompilationArguments(
                parentClassLoader = comCL?.classLoader, parentFileManager = comCL?.fileManager
            )
        ).classLoader

    return TestGenerator(
        refCL.loadClass(className),
        bytecodeProvider = answerableBytecodeProvider(refCL),
        solutionName = solutionName
    ).loadSubmission(subCL.loadClass(className), bytecodeProvider = answerableBytecodeProvider(subCL))
        .runTests(nextLong(), TestEnvironment(jeedOutputCapturer, jeedSandbox()))
}
