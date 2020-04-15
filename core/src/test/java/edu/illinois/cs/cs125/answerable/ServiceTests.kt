package edu.illinois.cs.cs125.answerable

import edu.illinois.cs.cs125.answerable.api.Answerable
import examples.binarytree.size.YourBinaryTree
import org.junit.jupiter.api.Assertions.*
import examples.binarytree.reference.YourBinaryTree as ReferenceYBT
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

fun TestRunOutput.assertAllSucceeded(showOutput: Boolean = true) {
    if (showOutput) println(this.toJson())
    this.classDesignAnalysisResult.forEach { assertTrue(it.result is Matched, "Class design check failed") }
    this.testSteps.forEach {
        if (it is ExecutedTestStep) {
            assertNull(it.assertErr, "Test failed")
            assertTrue(it.succeeded, "Test failed")
        }
    }
    assertTrue(this.testSteps.any { it is ExecutedTestStep }, "No tests were executed")
}

internal class ServiceTests {

    var answerableService: Answerable = Answerable(defaultEnvironment)

    @BeforeEach
    fun setUp() {
        answerableService = Answerable(defaultEnvironment)
    }

    @Test
    fun testAnswerableMisuseExceptionUpdate() {
        val errMsg: String = assertThrows<AnswerableMisuseException> {
            answerableService.loadNewQuestion(
                "MissingGenerator",
                referenceClass = examples.testgeneration.generators.errors.reference.MissingGenerator::class.java
            )
        }.message!!

        assertEquals("\nA generator for type `java.lang.StringBuilder' was requested, but no generator for that type was found.\n" +
                "While trying to load new question: MissingGenerator.", errMsg)
    }

    @Test
    fun testAnswerableVerificationExceptionUpdate() {
        val errMsg: String = assertThrows<AnswerableVerificationException> {
            answerableService.loadNewQuestion(
                "ImpossibleToPass",
                referenceClass = examples.verify.FailsAgainstSelf::class.java
            )
        }.message!!

        assertEquals("\nTesting reference against itself failed on inputs: []\n" +
                "While trying to load new question: ImpossibleToPass.", errMsg)
    }

    @Test
    fun testAnswerableBytecodeVerificationExceptionUpdate() {
        val errMsg: String = assertThrows<AnswerableVerificationException> {
            answerableService.loadNewQuestion(
                "BadFieldAccessWidget",
                referenceClass = examples.verify.BadFieldAccessWidget::class.java
            )
        }.message!!

        assertEquals("\nMirrorable method `generate' in `BadFieldAccessWidget' uses non-public submission field: numSprings\n" +
                "While trying to load new question: BadFieldAccessWidget.", errMsg)
    }

    @Test
    fun testTimeOut() {
        answerableService.loadNewQuestion("Timeout", examples.testgeneration.timeout.reference.TimeOut::class.java)
        val o = answerableService.submitAndTest("Timeout", examples.testgeneration.timeout.TimeOut::class.java)
        assertTrue(o.timedOut, "Failed to timeout: ")
    }

    @Test
    fun testYourBinaryTreeSize() {
        answerableService.loadNewQuestion("YourBinaryTree size", ReferenceYBT::class.java, "size")
        answerableService.submitAndTest("YourBinaryTree size", YourBinaryTree::class.java).assertAllSucceeded()
    }

    @Test
    fun testYourBinaryTreeSum() {
        answerableService.loadNewQuestion("YourBinaryTree sum", ReferenceYBT::class.java, "sum")
        answerableService.submitAndTest("YourBinaryTree sum", examples.binarytree.sum.YourBinaryTree::class.java)
            .assertAllSucceeded()
    }

    @Test
    fun testPrintOutput() {
        answerableService.loadNewQuestion("Printer", examples.printer.correct.reference.Printer::class.java)
        answerableService.submitAndTest("Printer", examples.printer.correct.Printer::class.java).assertAllSucceeded()
    }
}