package edu.illinois.cs.cs125.answerable

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class KotlinTest {

    private fun assertClassDesignPasses(solution: Class<*>, submission: Class<*>) {
        val analyzer = ClassDesignAnalysis("", solution, submission)
        Assertions.assertTrue(analyzer.runSuite().all { it.result is Matched })
    }

    private fun assertClassDesignFails(solution: Class<*>, submission: Class<*>) {
        val results = ClassDesignAnalysis("", solution, submission).runSuite()
        Assertions.assertFalse(results.all { it.result is Matched })
        results.filter { it.result is Mismatched }.forEach { println(it.toErrorMsg()) }
    }

    @Test
    fun testAverageClassDesign() {
        assertClassDesignPasses(examples.ktaverage.reference.Average::class.java, examples.ktaverage.Average::class.java)
    }

    @Test
    fun testDefaultCtorClassDesign() {
        assertClassDesignPasses(examples.ktclassdesign.correctctor.reference.DefaultConstructorWidget::class.java,
                examples.ktclassdesign.correctctor.DefaultConstructorWidget::class.java)
    }

    @Test
    fun testDefaultCtorMissingVal() {
        assertClassDesignFails(examples.ktclassdesign.correctctor.reference.DefaultConstructorWidget::class.java,
                examples.ktclassdesign.ctormissingval.DefaultConstructorWidget::class.java)
    }

    private fun assertAllSucceeded(results: TestRunOutput) {
        results.testSteps.forEach {
            if (it is ExecutedTestStep) {
                Assertions.assertNull(it.assertErr)
                Assertions.assertTrue(it.succeeded)
            }
        }
    }

    private fun assertClassesPass(solution: Class<*>, submission: Class<*>) {
        assertAllSucceeded(PassedClassDesignRunner(solution, submission).runTestsUnsecured(0x0403))
    }

    @Test
    fun testAverage() {
        assertClassesPass(examples.ktaverage.reference.Average::class.java, examples.ktaverage.Average::class.java)
    }

}