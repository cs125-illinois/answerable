package edu.illinois.cs.cs125.answerable

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class ServiceTests {

    var answerableService: Answerable = Answerable()

    @BeforeEach
    fun setUp() {
        answerableService = Answerable()
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

        assertEquals("Testing reference against itself failed on inputs: []\n" +
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

        assertEquals("Mirrorable method `generate' in `BadFieldAccessWidget' uses non-public submission field: numSprings\n" +
                "While trying to load new question: BadFieldAccessWidget.", errMsg)
    }
}