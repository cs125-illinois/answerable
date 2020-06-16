package edu.illinois.cs.cs125.answerable

import edu.illinois.cs.cs125.answerable.classmanipulation.AnswerableBytecodeVerificationException
import edu.illinois.cs.cs125.answerable.classmanipulation.verifyMemberAccess
import examples.proxy.reference.GeneratedWidget
import examples.proxy.reference.InnerClassGeneratorWidget
import examples.proxy.reference.RequiredInnerClassWidget
import examples.proxy.reference.StaticInitGeneratorWidget
import examples.verify.BadArrayParameterizedMethodAccessWidget
import examples.verify.BadConstructorAccess
import examples.verify.BadFieldAccessFromInnerWidget
import examples.verify.BadFieldAccessWidget
import examples.verify.BadMethodAccessFromInnerWidget
import examples.verify.BadMethodAccessWidget
import examples.verify.BadParameterizedMethodAccessWidget
import examples.verify.BadPrimitiveArrayParameterizedMethodAccessWidget
import examples.verify.BadPrimitiveParameterizedMethodAccessWidget
import examples.verify.IncidentalInnerClassWidget
import examples.verify.OverloadedSafeMethodAccessWidget
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
        assertThrows<AnswerableBytecodeVerificationException> {
            verifyMemberAccess(BadParameterizedMethodAccessWidget::class.java)
        }
    }

    @Test
    fun verifyBadPrimitiveParameterizedMethodAccessNext() {
        assertThrows<AnswerableBytecodeVerificationException> {
            verifyMemberAccess(BadPrimitiveParameterizedMethodAccessWidget::class.java)
        }
    }

    @Test
    fun verifyBadPrimitiveArrayParameterizedMethodAccessNext() {
        assertThrows<AnswerableBytecodeVerificationException> {
            verifyMemberAccess(BadPrimitiveArrayParameterizedMethodAccessWidget::class.java)
        }
    }

    @Test
    fun verifyBadArrayParameterizedMethodAccessNext() {
        assertThrows<AnswerableBytecodeVerificationException> {
            verifyMemberAccess(BadArrayParameterizedMethodAccessWidget::class.java)
        }
    }

    @Test
    fun verifySafeOverloadedMethodAccessGenerator() {
        verifyMemberAccess(OverloadedSafeMethodAccessWidget::class.java)
    }

    @Test
    fun verifyBadFieldAccessFromInnerGenerator() {
        assertThrows<AnswerableBytecodeVerificationException> {
            verifyMemberAccess(BadFieldAccessFromInnerWidget::class.java)
        }
    }

    @Test
    fun verifyBadMethodCallFromInnerGenerator() {
        assertThrows<AnswerableBytecodeVerificationException> {
            verifyMemberAccess(BadMethodAccessFromInnerWidget::class.java)
        }
    }

    @Test
    fun verifyRequiredInnerClassGenerator() {
        verifyMemberAccess(RequiredInnerClassWidget::class.java)
    }

    @Test
    fun verifyStaticInitGeneratorWidget() {
        verifyMemberAccess(StaticInitGeneratorWidget::class.java)
    }

    @Test
    fun verifyBadConstructorAccess() {
        assertThrows<AnswerableBytecodeVerificationException> {
            verifyMemberAccess(BadConstructorAccess::class.java)
        }
    }
}
