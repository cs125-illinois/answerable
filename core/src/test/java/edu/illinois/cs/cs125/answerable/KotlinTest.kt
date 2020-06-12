package edu.illinois.cs.cs125.answerable

import examples.adder.correct.reference.Adder
import examples.testgeneration.KtPrecondition
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class KotlinTest {

    @Test
    fun testLanguageModeDetection() {
        Assertions.assertSame(JavaMode, getLanguageMode(Adder::class.java))
        Assertions.assertSame(KotlinMode, getLanguageMode(KtPrecondition::class.java))
    }

    @Test
    fun testAverageClassDesign() {
        assertClassDesignPasses(examples.ktaverage.reference.Average::class.java, examples.ktaverage.Average::class.java)
    }

    @Test
    fun testDefaultCtorClassDesign() {
        assertClassDesignPasses(
            examples.ktclassdesign.correctctor.reference.DefaultConstructorWidget::class.java,
            examples.ktclassdesign.correctctor.DefaultConstructorWidget::class.java
        )
    }

    @Test
    fun testDefaultCtorMissingVal() {
        assertClassDesignFails(
            examples.ktclassdesign.correctctor.reference.DefaultConstructorWidget::class.java,
            examples.ktclassdesign.ctormissingval.DefaultConstructorWidget::class.java
        )
    }

    @Test
    fun testStringFilterClassDesign() {
        assertClassDesignPasses(
            examples.testgeneration.ktfilter.reference.StringFilterer::class.java,
            examples.testgeneration.ktfilter.StringFilterer::class.java
        )
    }

    private fun assertClassesPass(solution: Class<*>, submission: Class<*>) {
        PassedClassDesignRunner(solution, submission).runTestsUnsecured(0x0403).assertAllSucceeded()
    }

    @Test
    fun testAverage() {
        assertClassesPass(examples.ktaverage.reference.Average::class.java, examples.ktaverage.Average::class.java)
    }

    @Test
    fun testStringFilter() {
        assertClassesPass(examples.testgeneration.ktfilter.reference.StringFilterer::class.java, examples.testgeneration.ktfilter.StringFilterer::class.java)
    }

    @Test
    fun testPrecondition() {
        assertClassesPass(examples.testgeneration.reference.KtPrecondition::class.java, examples.testgeneration.KtPrecondition::class.java)
    }

    @Test
    fun testStandaloneVerify() {
        assertClassesPass(
            examples.testgeneration.standaloneverify.reference.KtStandaloneVerify::class.java,
            examples.testgeneration.standaloneverify.KtStandaloneVerify::class.java
        )
    }
}
