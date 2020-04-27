package edu.illinois.cs.cs125.answerable

import edu.illinois.cs.cs125.answerable.annotations.DEFAULT_EMPTY_NAME
import edu.illinois.cs.cs125.jeed.core.CompilationArguments
import edu.illinois.cs.cs125.jeed.core.Source
import edu.illinois.cs.cs125.jeed.core.compile
import kotlin.random.Random.Default.nextLong

fun testFromStrings(
    reference: String,
    submission: String,
    common: String? = null,
    className: String,
    solutionName: String = DEFAULT_EMPTY_NAME
): TestingResults {

    val referenceSource: Source = Source(mapOf("Reference.java" to reference))
    val submissionSource: Source = Source(mapOf("Submission.java" to submission))

    val commonSource: Source? = common?.run { Source(mapOf("Common.java" to common)) }

    val comCL = commonSource?.compile()
    val refCL = referenceSource
            .compile(CompilationArguments(parentClassLoader = comCL?.classLoader, parentFileManager = comCL?.fileManager))
            .classLoader
    val subCL = submissionSource
            .compile(CompilationArguments(parentClassLoader = comCL?.classLoader, parentFileManager = comCL?.fileManager))
            .classLoader

    return TestGenerator(
        refCL.loadClass(className),
        bytecodeProvider = answerableBytecodeProvider(refCL),
        solutionName = solutionName
    ).loadSubmission(subCL.loadClass(className), bytecodeProvider = answerableBytecodeProvider(subCL))
        .runTests(nextLong(), TestEnvironment(jeedOutputCapturer, jeedSandbox()))
}
