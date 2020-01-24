package edu.illinois.cs.cs125.answerable

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class KotlinTest {

    @Test
    fun testAverageClassDesign() {
        val analyzer = ClassDesignAnalysis("", examples.ktaverage.reference.Average::class.java, examples.ktaverage.Average::class.java)
        Assertions.assertTrue(analyzer.runSuite().all { it.result is Matched })
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