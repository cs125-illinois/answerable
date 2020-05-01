@file:Suppress("unused")

package edu.illinois.cs.cs125.answerable.jeedrunner

import edu.illinois.cs.cs125.answerable.AnswerableMisuseException
import edu.illinois.cs.cs125.answerable.AnswerableVerificationException
import edu.illinois.cs.cs125.answerable.TestGenerator
import edu.illinois.cs.cs125.answerable.TestRunnerArgs
import edu.illinois.cs.cs125.answerable.TestingResults
import edu.illinois.cs.cs125.answerable.annotations.DEFAULT_EMPTY_NAME
import edu.illinois.cs.cs125.answerable.api.BytecodeProvider
import edu.illinois.cs.cs125.answerable.defaultArgs
import edu.illinois.cs.cs125.jeed.core.CompilationArguments
import edu.illinois.cs.cs125.jeed.core.CompilationFailed
import edu.illinois.cs.cs125.jeed.core.CompiledSource
import edu.illinois.cs.cs125.jeed.core.KompilationArguments
import edu.illinois.cs.cs125.jeed.core.Sandbox
import edu.illinois.cs.cs125.jeed.core.Source
import edu.illinois.cs.cs125.jeed.core.compile
import edu.illinois.cs.cs125.jeed.core.kompile
import kotlin.random.Random

class Answerable {

    private val questions = mutableMapOf<String, Question>()

    @JvmOverloads
    @Throws(CompilationFailed::class)
    fun loadNewQuestion(
        questionName: String,
        language: QuestionLanguage,
        referenceCode: String,
        className: String,
        solutionName: String = DEFAULT_EMPTY_NAME,
        commonCode: List<String> = listOf(),
        testRunnerArgs: TestRunnerArgs = defaultArgs,
        classLoaderConfiguration: Sandbox.ClassLoaderConfiguration = Sandbox.ClassLoaderConfiguration(),
        executionArguments: Sandbox.ExecutionArguments = Sandbox.ExecutionArguments()
    ) {
        val common = if (commonCode.any()) compile(commonCode, "Common", language) else null
        val refCL = compile(listOf(referenceCode), "Reference", language, common).classLoader
        loadNewQuestionInternal(
            questionName = questionName,
            referenceClass = refCL.loadClass(className),
            solutionName = solutionName,
            language = language,
            bytecodeProvider = answerableBytecodeProvider(refCL),
            commonSource = common,
            testRunnerArgs = testRunnerArgs,
            classLoaderConfiguration = classLoaderConfiguration,
            executionArguments = executionArguments
        )
    }

    @JvmOverloads
    fun loadNewQuestion(
        questionName: String,
        referenceClass: Class<*>,
        solutionName: String = DEFAULT_EMPTY_NAME,
        language: QuestionLanguage? = null,
        testRunnerArgs: TestRunnerArgs = defaultArgs,
        classLoaderConfiguration: Sandbox.ClassLoaderConfiguration = Sandbox.ClassLoaderConfiguration(),
        executionArguments: Sandbox.ExecutionArguments = Sandbox.ExecutionArguments(),
        bytecodeProvider: BytecodeProvider? = null
    ) {
        loadNewQuestionInternal(
            questionName = questionName,
            referenceClass = referenceClass,
            solutionName = solutionName,
            language = language,
            bytecodeProvider = bytecodeProvider,
            commonSource = null,
            testRunnerArgs = testRunnerArgs,
            classLoaderConfiguration = classLoaderConfiguration,
            executionArguments = executionArguments
        )
    }

    private fun loadNewQuestionInternal(
        questionName: String,
        referenceClass: Class<*>,
        solutionName: String,
        language: QuestionLanguage?,
        bytecodeProvider: BytecodeProvider?,
        commonSource: CompiledSource?,
        testRunnerArgs: TestRunnerArgs,
        classLoaderConfiguration: Sandbox.ClassLoaderConfiguration,
        executionArguments: Sandbox.ExecutionArguments
    ) {
        require(!questions.containsKey(questionName)) { "A reference is already loaded for question $questionName" }
        val testGenerator = prettifyPotentialTestGeneratorError(questionName) {
            TestGenerator(
                referenceClass,
                solutionName = solutionName,
                bytecodeProvider = bytecodeProvider,
                testRunnerArgs = testRunnerArgs
            )
        }
        questions[questionName] = Question(
            testGenerator = testGenerator,
            language = language,
            commonSource = commonSource,
            classLoaderConfiguration = classLoaderConfiguration,
            executionConfiguration = executionArguments
        )
    }

    fun unloadQuestion(questionName: String): Boolean {
        return questions.remove(questionName) != null
    }

    @JvmOverloads
    @Throws(CompilationFailed::class)
    fun submit(
        questionName: String,
        submissionCode: String,
        overrideLanguage: QuestionLanguage? = null,
        testRunnerArgs: TestRunnerArgs = defaultArgs
    ): JeedTestRunner {
        val question = questions[questionName] ?: error("No question named $questionName is currently loaded")
        if (question.language == null && overrideLanguage == null) {
            error("Reference $questionName does not have a language, so one must be specified as overrideLanguage")
        }
        val submissionCL = compile(
            code = listOf(submissionCode),
            fileTitle = "Submission",
            language = overrideLanguage ?: question.language ?: error("Impossible: language is no longer set"),
            parentSource = question.commonSource
        ).classLoader
        val testRunner = question.testGenerator.loadSubmission(
            submissionClass = submissionCL.loadClass(question.testGenerator.referenceClass.simpleName),
            bytecodeProvider = answerableBytecodeProvider(submissionCL),
            testRunnerArgs = testRunnerArgs
        )
        return JeedTestRunner(testRunner, question.createJeedEnvironment())
    }

    @JvmOverloads
    fun submit(
        questionName: String,
        submissionClass: Class<*>,
        testRunnerArgs: TestRunnerArgs = defaultArgs,
        bytecodeProvider: BytecodeProvider? = null
    ): JeedTestRunner {
        val question = questions[questionName] ?: error("No question named $questionName is currently loaded")
        val testRunner = question.testGenerator.loadSubmission(
            submissionClass = submissionClass,
            bytecodeProvider = bytecodeProvider,
            testRunnerArgs = testRunnerArgs
        )
        return JeedTestRunner(testRunner, question.createJeedEnvironment())
    }

    @JvmOverloads
    @Throws(CompilationFailed::class)
    fun submitAndTest(
        questionName: String,
        submissionCode: String,
        seed: Long = Random.nextLong(),
        overrideLanguage: QuestionLanguage? = null,
        testRunnerArgs: TestRunnerArgs = defaultArgs
    ): TestingResults {
        return submit(questionName, submissionCode, overrideLanguage, testRunnerArgs).runTests(seed = seed)
    }

    @JvmOverloads
    fun submitAndTest(
        questionName: String,
        submissionClass: Class<*>,
        seed: Long = Random.nextLong(),
        testRunnerArgs: TestRunnerArgs = defaultArgs,
        bytecodeProvider: BytecodeProvider? = null
    ): TestingResults {
        return submit(questionName, submissionClass, testRunnerArgs, bytecodeProvider).runTests(seed = seed)
    }

    private fun compile(
        code: List<String>,
        fileTitle: String,
        language: QuestionLanguage,
        parentSource: CompiledSource? = null
    ): CompiledSource {
        val source = Source(code.mapIndexed { i, file ->
            "$fileTitle$i.${language.extension}" to file
        }.toMap())
        return when (language) {
            QuestionLanguage.JAVA -> {
               source.compile(CompilationArguments(
                   parentClassLoader = parentSource?.classLoader,
                   parentFileManager = parentSource?.fileManager
               ))
            }
            QuestionLanguage.KOTLIN -> {
                source.kompile(KompilationArguments(
                    parentClassLoader = parentSource?.classLoader ?: ClassLoader.getSystemClassLoader()
                ))
            }
        }
    }

    private inline fun prettifyPotentialTestGeneratorError(
        questionName: String,
        block: () -> TestGenerator
    ): TestGenerator {
        return try {
            block()
        } catch (ame: AnswerableMisuseException) {
            throw AnswerableMisuseException(
                "${ame.message?.trim()}\nWhile trying to load new question: $questionName.", ame
            )
        } catch (ave: AnswerableVerificationException) {
            throw AnswerableVerificationException(
                "${ave.message?.trim()}\nWhile trying to load new question: $questionName.", ave
            )
        }
    }

}

/**
 * Used as a bogus version in @SinceKotlin annotations to suppress Java-only overloads from Kotlin autocomplete.
 */
internal const val JAVA_ONLY: String = "125.0"
