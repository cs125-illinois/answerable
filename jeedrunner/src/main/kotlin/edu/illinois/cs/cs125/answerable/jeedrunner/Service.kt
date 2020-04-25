package edu.illinois.cs.cs125.answerable.jeedrunner

import edu.illinois.cs.cs125.answerable.AnswerableMisuseException
import edu.illinois.cs.cs125.answerable.AnswerableVerificationException
import edu.illinois.cs.cs125.answerable.TestGenerator
import edu.illinois.cs.cs125.answerable.TestRunnerArgs
import edu.illinois.cs.cs125.answerable.TestingResults
import edu.illinois.cs.cs125.answerable.answerableBytecodeProvider
import edu.illinois.cs.cs125.answerable.defaultArgs
import edu.illinois.cs.cs125.jeed.core.CompilationArguments
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
    fun loadNewQuestion(
        questionName: String,
        language: QuestionLanguage,
        referenceCode: String,
        className: String,
        solutionName: String = "",
        commonCode: String? = null,
        testRunnerArgs: TestRunnerArgs = defaultArgs,
        classLoaderConfiguration: Sandbox.ClassLoaderConfiguration = Sandbox.ClassLoaderConfiguration(),
        executionArguments: Sandbox.ExecutionArguments = Sandbox.ExecutionArguments()
    ) {
        require(!questions.containsKey(questionName)) { "A reference is already loaded for question $questionName" }
        val common = commonCode?.let { compile(commonCode, "Common", language) }
        val refCL = compile(referenceCode, "Reference", language, common).classLoader
        val testGenerator = prettifyPotentialTestGeneratorError(questionName) {
            TestGenerator(
                refCL.loadClass(className),
                solutionName = solutionName,
                bytecodeProvider = answerableBytecodeProvider(refCL),
                testRunnerArgs = testRunnerArgs
            )
        }
        questions[questionName] = Question(
            testGenerator = testGenerator,
            language = language,
            commonSource = common,
            classLoaderConfiguration = classLoaderConfiguration,
            executionConfiguration = executionArguments
        )
    }

    fun unloadQuestion(questionName: String): Boolean {
        return questions.remove(questionName) != null
    }

    @JvmOverloads
    fun submitAndTest(
        questionName: String,
        submissionCode: String,
        seed: Long = Random.nextLong(),
        overrideLanguage: QuestionLanguage? = null,
        testRunnerArgs: TestRunnerArgs = defaultArgs
    ): TestingResults {
        val question = questions[questionName] ?: error("No question named $questionName is currently loaded")
        if (question.language == null && overrideLanguage == null) {
            error("Reference $questionName does not have a language, so one must be specified as overrideLanguage")
        }
        val submissionCL = compile(
            code = submissionCode,
            fileTitle = "Submission",
            language = overrideLanguage ?: question.language ?: error("Impossible: language is no longer set"),
            parentSource = question.commonSource
        ).classLoader
        val testRunner = question.testGenerator.loadSubmission(
            submissionClass = submissionCL.loadClass(question.testGenerator.referenceClass.name),
            bytecodeProvider = answerableBytecodeProvider(submissionCL)
        )
        return testRunner.runTests(seed, question.createJeedEnvironment(), testRunnerArgs)
    }

    private fun compile(
        code: String,
        fileTitle: String,
        language: QuestionLanguage,
        parentSource: CompiledSource? = null
    ): CompiledSource {
        val source = Source(mapOf("$fileTitle.${language.extension}" to code))
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
