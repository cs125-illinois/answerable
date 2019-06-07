package edu.illinois.cs.cs125.answerable

import examples.proxy.reference.GeneratedWidget
import examples.proxy.reference.InnerClassGeneratorWidget
import examples.verify.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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
    fun verifySafeImplementationInnerClassGenerator() {
        verifyMemberAccess(IncidentalInnerClassWidget::class.java)
    }

    @Test
    fun verifyBadFieldAccessGenerator() {
        assertThrows<AnswerableBytecodeVerificationException> { verifyMemberAccess(BadFieldAccessWidget::class.java) }
    }

    @Test
    fun verifyBadMethodAccessGenerator() {
        assertThrows<AnswerableBytecodeVerificationException> { verifyMemberAccess(BadMethodAccessWidget::class.java) }
    }

    @Test
    fun verifyBadParameterizedMethodAccessGenerator() {
        assertThrows<AnswerableBytecodeVerificationException> { verifyMemberAccess(BadParameterizedMethodAccessWidget::class.java) }
    }

    @Test
    fun verifyBadPrimitiveParameterizedMethodAccessNext() {
        assertThrows<AnswerableBytecodeVerificationException> { verifyMemberAccess(BadPrimitiveParameterizedMethodAccessWidget::class.java) }
    }

    @Test
    fun verifyBadPrimitiveArrayParameterizedMethodAccessNext() {
        assertThrows<AnswerableBytecodeVerificationException> { verifyMemberAccess(BadPrimitiveArrayParameterizedMethodAccessWidget::class.java) }
    }

    @Test
    fun verifyBadArrayParameterizedMethodAccessNext() {
        assertThrows<AnswerableBytecodeVerificationException> { verifyMemberAccess(BadArrayParameterizedMethodAccessWidget::class.java) }
    }

    @Test
    fun verifySafeOverloadedMethodAccessGenerator() {
        verifyMemberAccess(OverloadedSafeMethodAccessWidget::class.java)
    }

    @Test
    fun verifyBadFieldAccessFromInnerGenerator() {
        assertThrows<AnswerableBytecodeVerificationException> { verifyMemberAccess(BadFieldAccessFromInnerWidget::class.java) }
    }

    @Test
    fun verifyBadMethodCallFromInnerGenerator() {
        assertThrows<AnswerableBytecodeVerificationException> { verifyMemberAccess(BadMethodAccessFromInnerWidget::class.java) }
    }

}