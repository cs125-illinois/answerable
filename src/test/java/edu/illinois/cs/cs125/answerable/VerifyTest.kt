package edu.illinois.cs.cs125.answerable

import examples.proxy.reference.GeneratedWidget
import examples.proxy.reference.InnerClassGeneratorWidget
import examples.verify.*
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

    @Test
    fun verifyBadMethodAccessGenerator() {
        Assertions.assertThrows(AnswerableMisuseException::class.java) { verifyMemberAccess(BadMethodAccessWidget::class.java) }
    }

    @Test
    fun verifyBadParameterizedMethodAccessGenerator() {
        Assertions.assertThrows(AnswerableMisuseException::class.java) { verifyMemberAccess(BadParameterizedMethodAccessWidget::class.java) }
    }

    @Test
    fun verifyBadPrimitiveParameterizedMethodAccessNext() {
        Assertions.assertThrows(AnswerableMisuseException::class.java) { verifyMemberAccess(BadPrimitiveParameterizedMethodAccessWidget::class.java) }
    }

    @Test
    fun verifyOverloadedMethodAccessGenerator() {
        verifyMemberAccess(OverloadedSafeMethodAccessWidget::class.java)
    }

    @Test
    fun verifyBadFieldAccessFromInnerGenerator() {
        Assertions.assertThrows(AnswerableMisuseException::class.java) { verifyMemberAccess(BadFieldAccessFromInnerWidget::class.java) }
    }

}