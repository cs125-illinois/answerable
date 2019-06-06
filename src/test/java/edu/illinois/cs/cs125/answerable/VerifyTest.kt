package edu.illinois.cs.cs125.answerable

import examples.proxy.reference.GeneratedWidget
import examples.proxy.reference.InnerClassGeneratorWidget
import examples.verify.BadFieldAccessWidget
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class VerifyTest {

    @Test
    fun verifySafeGenerator() {
        verifyMemberAccess(GeneratedWidget::class.java)
    }

    @Test
    fun verifySafeInnerClassGenerator() {
        verifyMemberAccess(InnerClassGeneratorWidget::class.java)
    }

    @Test
    fun verifyBadFieldAccessGenerator() {
        Assertions.assertThrows(AnswerableMisuseException::class.java) { verifyMemberAccess(BadFieldAccessWidget::class.java) }
    }

}