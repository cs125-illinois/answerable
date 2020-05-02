@file:Suppress("unused")

package edu.illinois.cs.cs125.answerable.jeedrunner

import edu.illinois.cs.cs125.answerable.AnswerableMisuseException
import edu.illinois.cs.cs125.answerable.AnswerableVerificationException
import edu.illinois.cs.cs125.answerable.TestGenerator
import edu.illinois.cs.cs125.answerable.TestRunnerArgs
import edu.illinois.cs.cs125.answerable.TestingResults
import edu.illinois.cs.cs125.answerable.annotations.DEFAULT_EMPTY_NAME
import edu.illinois.cs.cs125.answerable.annotations.Solution
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

/**
 * Facilitates use of Answerable as a service by compiling code with Jeed, keeping track of questions, and running
 * submissions in the Jeed sandbox.
 */
class Answerable {

    /** Currently loaded questions. */
    private val questions = mutableMapOf<String, Question>()

    /**
     * Loads a new question into the Answerable service with code for the reference solution.
     * Submissions can then be tested against it with [submit]/[submitAndTest].
     *
     * @throws IllegalArgumentException if the specified question name is already registered
     * @throws CompilationFailed if the [referenceCode] or [commonCode] could not be compiled
     * @throws AnswerableMisuseException if the reference solution code does not specify Answerable settings properly
     * @throws AnswerableVerificationException if control functions rely on submission-specific members
     *
     * @param questionName the name to save this question as
     * @param language the [QuestionLanguage] the reference solution is written in
     * @param referenceCode code for the question and reference solution, in the [language] specified
     * @param className the name of the class containing the @[Solution] annotation
     * @param solutionName the specific solution name, if the question class contains multiple problems
     * @param commonCode files containing any code usable by both reference and submission
     * @param testRunnerArgs default test run arguments for the problem
     * @param classLoaderConfiguration Jeed classloading restrictions for submissions
     * @param executionArguments Jeed sandbox restrictions for submissions
     */
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

    /**
     * Loads a new question into the Answerable service with an already compiled reference solution class.
     * Submissions can then be tested against it with [submit]/[submitAndTest].
     *
     * @throws IllegalArgumentException if the specified question name is already registered
     * @throws AnswerableMisuseException if the reference solution code does not specify Answerable settings properly
     * @throws AnswerableVerificationException if control functions rely on submission-specific members
     *
     * @param questionName the name to save this question as
     * @param referenceClass the outer class containing the @[Solution]
     * @param solutionName the specific solution name, if the question class contains multiple problems
     * @param language the default language to use when compiling submission code
     * @param testRunnerArgs default test run arguments for the problem
     * @param classLoaderConfiguration Jeed classloading restrictions for submissions
     * @param executionArguments Jeed sandbox restrictions for submissions
     * @param bytecodeProvider the [BytecodeProvider] for the [referenceClass], if it was dynamically loaded
     */
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

    /**
     * Removes a question from this service's records.
     *
     * @param questionName the name of the question to unload
     * @return whether there was a question of the specified name
     */
    fun unloadQuestion(questionName: String): Boolean {
        return questions.remove(questionName) != null
    }

    /**
     * Makes a code submission to a question, producing a [JeedTestRunner] which can run sandboxed tests on demand.
     * By default the submission code will be assumed to be in the same language as the original reference. This can
     * be overridden, and must be if the reference was loaded from a compiled class with no language specified.
     * The submission class must be outside any package and must have the same unqualified name as the reference class.
     *
     * @throws IllegalArgumentException if there is no currently loaded question with the specified name,
     *                                  or if no code language was specified here or during question loading
     * @throws CompilationFailed if the [submissionCode] could not be compiled
     *
     * @param questionName the name of the question this submission is to
     * @param submissionCode the submission code
     * @param overrideLanguage the language to interpret [submissionCode] as
     * @param testRunnerArgs additional test run arguments specific for running this submission
     * @return a test runner that will execute tests of this submission
     */
    @JvmOverloads
    @Throws(CompilationFailed::class)
    fun submit(
        questionName: String,
        submissionCode: String,
        overrideLanguage: QuestionLanguage? = null,
        testRunnerArgs: TestRunnerArgs = defaultArgs
    ): JeedTestRunner {
        val question = questions[questionName] ?: error("No question named $questionName is currently loaded")
        val actualLanguage = overrideLanguage ?: question.language
            ?: error("Reference $questionName does not have a language, so one must be specified as overrideLanguage")
        val submissionCL = compile(
            code = listOf(submissionCode),
            fileTitle = "Submission",
            language = actualLanguage,
            parentSource = question.commonSource
        ).classLoader
        val testRunner = question.testGenerator.loadSubmission(
            submissionClass = submissionCL.loadClass(question.testGenerator.referenceClass.simpleName),
            bytecodeProvider = answerableBytecodeProvider(submissionCL),
            testRunnerArgs = testRunnerArgs
        )
        return JeedTestRunner(testRunner, question.createJeedEnvironment())
    }

    /**
     * Makes a submission to a question from a precompiled class, producing a [JeedTestRunner] which can run
     * sandboxed tests on demand. Any common classes must have been loaded by the parent classloader of the submission
     * class's loader.
     *
     * @throws IllegalArgumentException if there is no currently loaded question with the specified name
     *
     * @param questionName the name of the question this submission is to
     * @param submissionClass the precompiled submission class
     * @param testRunnerArgs additional test run arguments specific for running this submission
     * @param bytecodeProvider the [BytecodeProvider] for the [submissionClass] if it was dynamically loaded
     * @return a test runner that will execute tests of this submission
     */
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

    /**
     * Calls [submit] and immediately tests the code submission with the given [seed].
     */
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

    /**
     * Calls [submit] and immediately tests the precompiled class submission with the given [seed].
     */
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
