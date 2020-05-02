@file:Suppress("UNCHECKED_CAST", "unused")

package edu.illinois.cs.cs125.answerable.jeedrunner

import edu.illinois.cs.cs125.answerable.TestRunnerArgs
import edu.illinois.cs.cs125.answerable.annotations.DEFAULT_EMPTY_NAME
import edu.illinois.cs.cs125.answerable.api.BytecodeProvider
import edu.illinois.cs.cs125.answerable.defaultArgs
import edu.illinois.cs.cs125.jeed.core.CompilationFailed
import edu.illinois.cs.cs125.jeed.core.Sandbox

abstract class QuestionBuilder<T : QuestionBuilder<T>> internal constructor(
    protected val service: Answerable,
    protected val questionName: String
) {

    protected var solutionName: String = DEFAULT_EMPTY_NAME
    protected var testRunnerArgs: TestRunnerArgs = defaultArgs
    protected var classLoaderConfiguration: Sandbox.ClassLoaderConfiguration = Sandbox.ClassLoaderConfiguration()
    protected var executionArguments: Sandbox.ExecutionArguments = Sandbox.ExecutionArguments()

    fun solutionName(setSolutionName: String): T {
        return this.also { solutionName = setSolutionName } as T
    }

    fun testRunnerArgs(setTestRunnerArgs: TestRunnerArgs): T {
        return this.also { testRunnerArgs = setTestRunnerArgs } as T
    }

    fun classLoaderConfiguration(setConfiguration: Sandbox.ClassLoaderConfiguration): T {
        return this.also { classLoaderConfiguration = setConfiguration } as T
    }

    fun executeConfiguration(setArguments: Sandbox.ExecutionArguments): T {
        return this.also { executionArguments = setArguments } as T
    }

    abstract fun loadNewQuestion()

}

class CodeQuestionBuilder internal constructor(
    service: Answerable,
    questionName: String,
    private val questionLanguage: QuestionLanguage,
    private val referenceCode: String,
    private val className: String
) : QuestionBuilder<CodeQuestionBuilder>(service, questionName) {

    private var commonCode: List<String> = listOf()

    fun commonCode(setCommonCode: List<String>): CodeQuestionBuilder {
        return this.also { commonCode = setCommonCode }
    }

    fun commonCode(vararg setCommonCode: String): CodeQuestionBuilder {
        return this.also { commonCode = setCommonCode.toList() }
    }

    @Throws(CompilationFailed::class)
    override fun loadNewQuestion() {
        service.loadNewQuestion(
            questionName = questionName,
            language = questionLanguage,
            referenceCode = referenceCode,
            className = className,
            commonCode = commonCode,
            solutionName = solutionName,
            testRunnerArgs = testRunnerArgs,
            classLoaderConfiguration = classLoaderConfiguration,
            executionArguments = executionArguments
        )
    }

}

class ClassQuestionBuilder internal constructor(
    service: Answerable,
    questionName: String,
    private val referenceClass: Class<*>
): QuestionBuilder<ClassQuestionBuilder>(service, questionName) {

    private var language: QuestionLanguage? = null
    private var bytecodeProvider: BytecodeProvider? = null

    fun language(setLanguage: QuestionLanguage): ClassQuestionBuilder {
        return this.also { language = setLanguage }
    }

    fun bytecodeProvider(setBytecodeProvider: BytecodeProvider): ClassQuestionBuilder {
        return this.also { bytecodeProvider = setBytecodeProvider }
    }

    override fun loadNewQuestion() {
        service.loadNewQuestion(
            questionName = questionName,
            referenceClass = referenceClass,
            language = language,
            bytecodeProvider = bytecodeProvider,
            solutionName = solutionName,
            testRunnerArgs = testRunnerArgs,
            classLoaderConfiguration = classLoaderConfiguration,
            executionArguments = executionArguments
        )
    }

}

abstract class SubmissionBuilder<T : SubmissionBuilder<T>> internal constructor(
    protected val service: Answerable,
    protected val questionName: String
) {

    protected var testRunnerArgs: TestRunnerArgs = defaultArgs

    fun testRunnerArgs(setArgs: TestRunnerArgs): T {
        return this.also { testRunnerArgs = setArgs } as T
    }

    abstract fun submit(): JeedTestRunner

}

class CodeSubmissionBuilder internal constructor(
    service: Answerable,
    questionName: String,
    private val submissionCode: String
): SubmissionBuilder<CodeSubmissionBuilder>(service, questionName) {

    private var overrideLanguage: QuestionLanguage? = null

    fun overrideLanguage(setLanguage: QuestionLanguage): CodeSubmissionBuilder {
        return this.also { overrideLanguage = setLanguage }
    }

    @Throws(CompilationFailed::class)
    override fun submit(): JeedTestRunner {
        return service.submit(
            questionName = questionName,
            submissionCode = submissionCode,
            overrideLanguage = overrideLanguage,
            testRunnerArgs = testRunnerArgs
        )
    }

}

class ClassSubmissionBuilder internal constructor(
    service: Answerable,
    questionName: String,
    private val submissionClass: Class<*>
): SubmissionBuilder<ClassSubmissionBuilder>(service, questionName) {

    private var bytecodeProvider: BytecodeProvider? = null

    fun bytecodeProvider(setBytecodeProvider: BytecodeProvider): ClassSubmissionBuilder {
        return this.also { bytecodeProvider = setBytecodeProvider }
    }

    override fun submit(): JeedTestRunner {
        return service.submit(
            questionName = questionName,
            submissionClass = submissionClass,
            bytecodeProvider = bytecodeProvider,
            testRunnerArgs = testRunnerArgs
        )
    }

}
