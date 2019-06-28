package edu.illinois.cs.cs125.answerable.api

import edu.illinois.cs.cs125.answerable.*
import java.lang.IllegalStateException
import java.util.*

/**
 * Allows use of Answerable as a service. Stores a map from question names to [TestGenerator]s.
 */
class Answerable(private val environment: TestEnvironment) {
    private val existingQuestions: MutableMap<String, TestGenerator> = mutableMapOf()

    /**
     * Load a new question into the Answerable service.
     *
     * Throws [AnswerableMisuseException]s and [AnswerableVerificationException] if issues are found with the [referenceClass].
     * It is recommended to wrap calls to [loadNewQuestion] in a try-catch block.
     *
     * @throws [AnswerableMisuseException]
     * @throws [AnswerableVerificationException]
     *
     * @param questionName the name to save this question under.
     * @param referenceClass the reference class for this question.
     * @param solutionName the name of the @[Solution] or standalone @[Verify] method to use in this question.
     * @param testRunnerArgs the default arguments to [TestRunner]s produced by this question.
     * @param bytecodeProvider bytecode provider for reference solution class(es).
     * @param commonLoader the loader of dynamically loaded classes used by both the reference and submission.
     */
    @JvmOverloads
    fun loadNewQuestion(
        questionName: String,
        referenceClass: Class<*>,
        solutionName: String = "",
        testRunnerArgs: TestRunnerArgs = defaultArgs,
        bytecodeProvider: BytecodeProvider? = null,
        commonLoader: ClassLoader? = null
    ) {
        val testgen: TestGenerator
        try {
            testgen = TestGenerator(referenceClass, solutionName, testRunnerArgs, bytecodeProvider, commonLoader)
        } catch (ame: AnswerableMisuseException) {
            throw AnswerableMisuseException("${ame.message?.trim()}\nWhile trying to load new question: $questionName.")
                .initCause(ame)
        } catch (ave: AnswerableVerificationException) {
            throw AnswerableVerificationException("${ave.message?.trim()}\nWhile trying to load new question: $questionName.")
                .initCause(ave)
        }

        existingQuestions[questionName] = testgen
    }

    // NOTE: [Overloads] @JvmOverloads will only remove optional parameters from the end, so we need to declare other overloads manually:
    @JvmOverloads
    fun loadNewQuestion(
        questionName: String,
        referenceClass: Class<*>,
        testRunnerArgs: TestRunnerArgs,
        bytecodeProvider: BytecodeProvider? = null,
        commonLoader: ClassLoader? = null
    ) = loadNewQuestion(questionName, referenceClass, "", testRunnerArgs, bytecodeProvider, commonLoader)

    /**
     * Make a submission to a question. Returns a [TestRunner] which can run tests on demand.
     *
     * @param questionName the name of the question being submitted to
     * @param submissionClass the class being submitted
     * @param testRunnerArgs arguments to override the question's defaults, if any
     * @param bytecodeProvider bytecode provider for submission class(es)
     */
    @JvmOverloads
    fun submit(
        questionName: String,
        submissionClass: Class<*>,
        testRunnerArgs: TestRunnerArgs = defaultArgs,
        bytecodeProvider: BytecodeProvider? = null
    ): TestRunner =
        (existingQuestions[questionName] ?: throw IllegalStateException("No question with name `$questionName' is currently loaded."))
            .loadSubmission(submissionClass, testRunnerArgs, bytecodeProvider)

    fun submit(
        questionName: String,
        submissionClass: Class<*>,
        bytecodeProvider: BytecodeProvider?
    ) = submit(questionName, submissionClass, defaultArgs, bytecodeProvider)

    /**
     * [submit] a question and also execute the [TestRunner] with the given [seed].
     */
    fun submitAndTest(
        questionName: String,
        submissionClass: Class<*>,
        testRunnerArgs: TestRunnerArgs = defaultArgs,
        seed: Long = Random().nextLong()
    ): TestRunOutput = submit(questionName, submissionClass, testRunnerArgs).runTests(seed, environment)
    fun submitAndTest(
        questionName: String,
        submissionClass: Class<*>,
        testRunnerArgs: TestRunnerArgs
    ) = submitAndTest(questionName, submissionClass, testRunnerArgs, Random().nextLong())
    fun submitAndTest(
        questionName: String,
        submissionClass: Class<*>,
        seed: Long
    ) = submitAndTest(questionName, submissionClass, defaultArgs, seed)
    fun submitAndTest(
        questionName: String,
        submissionClass: Class<*>
    ) = submitAndTest(questionName, submissionClass, defaultArgs, Random().nextLong())
}