@file:Suppress("UNUSED", "MatchingDeclarationName", "MemberVisibilityCanBePrivate")

package edu.illinois.cs.cs125.answerable.api

import edu.illinois.cs.cs125.answerable.AnswerableMisuseException
import edu.illinois.cs.cs125.answerable.AnswerableVerificationException
import edu.illinois.cs.cs125.answerable.TestEnvironment
import edu.illinois.cs.cs125.answerable.TestGenerator
import edu.illinois.cs.cs125.answerable.TestingResults
import edu.illinois.cs.cs125.answerable.TestRunner
import edu.illinois.cs.cs125.answerable.TestRunnerArgs
import edu.illinois.cs.cs125.answerable.defaultArgs
import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import java.util.Random

/**
 * Allows use of Answerable as a service. Stores a map from question names to [TestGenerator]s.
 */
@Suppress("TooManyFunctions")
class Service(private val environment: TestEnvironment) {
    private val existingQuestions: MutableMap<String, TestGenerator> = mutableMapOf()

    /**
     * Load a new question into the Answerable service. If a question with the given name already exists,
     * raises an [IllegalArgumentException].
     *
     * Throws [AnswerableMisuseException]s and [AnswerableVerificationException] if issues are found with the
     * [referenceClass]. It is recommended to wrap calls to [loadNewQuestion] in a try-catch block.
     *
     * @throws [AnswerableMisuseException]
     * @throws [AnswerableVerificationException]
     *
     * @param questionName the name to save this question under.
     * @param referenceClass the reference class for this question.
     * @param solutionName the name of the @[Solution] or standalone @[Verify] method to use in this question.
     * @param testRunnerArgs the default arguments to [TestRunner]s produced by this question.
     * @param bytecodeProvider bytecode provider for reference solution class(es).
     */
    @Suppress("ThrowsCount")
    @JvmOverloads
    fun loadNewQuestion(
        questionName: String,
        referenceClass: Class<*>,
        solutionName: String = "",
        testRunnerArgs: TestRunnerArgs = defaultArgs,
        bytecodeProvider: BytecodeProvider? = null
    ) {
        if (existingQuestions.containsKey(questionName)) {
            throw IllegalArgumentException("Answerable already has a reference for question named `$questionName'.")
        }

        val testgen: TestGenerator
        try {
            testgen = TestGenerator(referenceClass, solutionName, testRunnerArgs, bytecodeProvider)
        } catch (ame: AnswerableMisuseException) {
            throw AnswerableMisuseException(
                "${ame.message?.trim()}\nWhile trying to load new question: $questionName.", ame
            )
        } catch (ave: AnswerableVerificationException) {
            throw AnswerableVerificationException(
                "${ave.message?.trim()}\nWhile trying to load new question: $questionName.", ave
            )
        }

        existingQuestions[questionName] = testgen
    }

    // NOTE: [Overloads] @JvmOverloads will only remove optional parameters from the end, so we need to declare
    // other overloads manually:
    @JvmOverloads
    fun loadNewQuestion(
        questionName: String,
        referenceClass: Class<*>,
        testRunnerArgs: TestRunnerArgs,
        bytecodeProvider: BytecodeProvider? = null
    ) = loadNewQuestion(questionName, referenceClass, "", testRunnerArgs, bytecodeProvider)

    fun unloadQuestion(questionName: String) {
        existingQuestions.remove(questionName)
    }

    private fun updateQuestionInternal(
        questionName: String,
        referenceClass: Class<*>,
        cb: (TestGenerator) -> Triple<String, TestRunnerArgs, BytecodeProvider?>
    ) {
        val oldTestGen = existingQuestions.remove(questionName)
        val extras = cb(oldTestGen ?: return)
        loadNewQuestion(questionName, referenceClass, extras.first, extras.second, extras.third)
    }

    /**
     * Update an existing question in the service. If the question doesn't already exist, exits silently.
     * Note: will _not_ register a new question if the question does not already exist.
     *
     * See [loadNewQuestion] for param information; if null is passed for [solutionName] or [testRunnerArgs],
     * the existing data from the old question will be used.
     */
    fun updateQuestion(
        questionName: String,
        referenceClass: Class<*>,
        solutionName: String? = null,
        testRunnerArgs: TestRunnerArgs? = null
    ) {
        updateQuestionInternal(questionName, referenceClass) {
            Triple(solutionName ?: it.solutionName, testRunnerArgs ?: it.mergedArgs, it.bytecodeProvider)
        }
    }
    // fill in the missing JvmOverload
    fun updateQuestion(
        questionName: String,
        referenceClass: Class<*>,
        testRunnerArgs: TestRunnerArgs
    ) = updateQuestionInternal(questionName, referenceClass) {
        Triple(it.solutionName, testRunnerArgs, it.bytecodeProvider)
    }

    /**
     * Fully-featured overload of [updateQuestion] which additionally enables modifying the bytecodeProvider.
     */
    fun updateQuestion(
        questionName: String,
        referenceClass: Class<*>,
        solutionName: String? = null,
        testRunnerArgs: TestRunnerArgs? = null,
        bytecodeProvider: BytecodeProvider?
    ) = updateQuestionInternal(questionName, referenceClass) {
        Triple(solutionName ?: it.solutionName, testRunnerArgs ?: it.mergedArgs, bytecodeProvider)
    }

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
    ): TestRunner = (existingQuestions[questionName]
    ?: throw IllegalStateException("No question with name `$questionName' is currently loaded."))
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
    ): TestingResults = submit(questionName, submissionClass, testRunnerArgs).runTests(seed, environment)
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
