package edu.illinois.cs.cs125.answerable

import java.lang.IllegalStateException
import java.util.*
import java.util.concurrent.ThreadLocalRandom

class Answerable {
    val existingQuestions: MutableMap<String, TestGenerator> = mutableMapOf()

    fun loadNewQuestion(
        questionName: String,
        solutionName: String = "",
        referenceClass: Class<*>,
        testRunnerArgs: TestRunnerArgs = defaultArgs
    ) {
        val testgen: TestGenerator
        try {
            testgen = TestGenerator(referenceClass, solutionName, testRunnerArgs)
        } catch (ame: AnswerableMisuseException) {
            throw AnswerableMisuseException("${ame.message?.trim()}\nWhile trying to load new question: $questionName.")
        } catch (ave: AnswerableVerificationException) {
            throw AnswerableVerificationException("${ave.message?.trim()}\nWhile trying to load new question: $questionName.")
        }

        existingQuestions[questionName] = testgen
    }

    // NOTE: [Overloads] In order for our overloads to properly reach the Java side, we have to declare them manually:
    fun loadNewQuestion(
        questionName: String,
        referenceClass: Class<*>,
        testRunnerArgs: TestRunnerArgs
    ) = loadNewQuestion(questionName, "", referenceClass, testRunnerArgs)
    fun loadNewQuestion(
        questionName: String,
        solutionName: String,
        referenceClass: Class<*>
    ) = loadNewQuestion(questionName, solutionName, referenceClass, defaultArgs)
    fun loadNewQuestion(
        questionName: String,
        referenceClass: Class<*>
    ) = loadNewQuestion(questionName, "", referenceClass, defaultArgs)

    fun submit(
        questionName: String,
        submissionClass: Class<*>,
        testRunnerArgs: TestRunnerArgs = defaultArgs
    ): TestRunner =
        (existingQuestions[questionName] ?: throw IllegalStateException("No question with name `$questionName' is currently loaded."))
            .loadSubmission(submissionClass, testRunnerArgs)
    fun submit(
        questionName: String,
        submissionClass: Class<*>
    ) = submit(questionName, submissionClass, defaultArgs)

    fun submitAndTest(
        questionName: String,
        submissionClass: Class<*>,
        testRunnerArgs: TestRunnerArgs = defaultArgs,
        seed: Long = Random().nextLong()
    ): List<TestStep> = submit(questionName, submissionClass, testRunnerArgs).runTests(seed)
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