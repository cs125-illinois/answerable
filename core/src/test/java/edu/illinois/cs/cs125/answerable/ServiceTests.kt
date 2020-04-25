package edu.illinois.cs.cs125.answerable

import edu.illinois.cs.cs125.answerable.api.Service
import examples.binarytree.reference.YourBinaryTree as ReferenceYBT
import examples.binarytree.size.YourBinaryTree
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class ServiceTests {

    private var answerableService: Service = Service(unsecuredEnvironment)

    @BeforeEach
    fun setUp() {
        answerableService = Service(unsecuredEnvironment)
    }

    @Test
    fun testAnswerableMisuseExceptionUpdate() {
        val errMsg: String = assertThrows<AnswerableMisuseException> {
            answerableService.loadNewQuestion(
                "MissingGenerator",
                referenceClass = examples.testgeneration.generators.errors.reference.MissingGenerator::class.java
            )
        }.message!!

        assertEquals(
            "\nA generator for type `java.lang.StringBuilder' was requested, " +
            "but no generator for that type was found.\n" +
            "While trying to load new question: MissingGenerator.",
            errMsg
        )
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

        assertEquals(
            "\nMirrorable method `generate' in `BadFieldAccessWidget' uses non-public submission field: " +
                "numSprings\n" +
                "While trying to load new question: BadFieldAccessWidget.",
            errMsg
        )
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
