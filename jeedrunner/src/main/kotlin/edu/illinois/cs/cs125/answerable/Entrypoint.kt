package edu.illinois.cs.cs125.answerable

import edu.illinois.cs.cs125.jeed.core.CompilationArguments
import edu.illinois.cs.cs125.jeed.core.CompilationFailed
import edu.illinois.cs.cs125.jeed.core.Source
import edu.illinois.cs.cs125.jeed.core.compile
import kotlin.random.Random.Default.nextLong

fun testFromStrings(
    reference: String,
    submission: String,
    common: String? = null,
    className: String,
    solutionName: String = ""
): TestRunOutput {

    val referenceSource: Source = Source(mapOf("Reference" to reference))
    val submissionSource: Source = Source(mapOf("Submission" to submission))

    val commonSource: Source? = common?.run { Source(mapOf("Common" to common)) }

    val comCL = try {
        commonSource?.compile()
    }  catch (e: CompilationFailed) {
        e.errors.forEach { println("${it.location}: ${it.message}\n") }
        throw e
    }

    val refCL = try {
        referenceSource.compile(CompilationArguments(parentClassLoader = comCL?.classLoader, parentFileManager = comCL?.fileManager))
    } catch (e: CompilationFailed) {
        e.errors.forEach { println("${it.location}: ${it.message}\n") }
        throw e
    }.classLoader
    val subCL = try {
        submissionSource.compile(CompilationArguments(parentClassLoader = comCL?.classLoader, parentFileManager = comCL?.fileManager))
    } catch (e: CompilationFailed) {
        e.errors.forEach { println("${it.location}: ${it.message}\n") }
        throw e
    }.classLoader

    return TestGenerator(
        refCL.loadClass(className),
        bytecodeProvider = answerableBytecodeProvider(refCL),
        solutionName = solutionName
    ).loadSubmission(subCL.loadClass(className), bytecodeProvider = answerableBytecodeProvider(subCL))
        .runTests(nextLong(), TestEnvironment(jeedOutputCapturer, jeedSandbox()))
}