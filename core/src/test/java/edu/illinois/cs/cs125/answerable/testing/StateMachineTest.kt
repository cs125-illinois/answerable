package edu.illinois.cs.cs125.answerable.testing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StateMachineTest {
    @Test
    fun `state machine runs the correct number of tests`() {
        val machine = StateMachine(defaultArgs.resolve())
        var count = 0
        while (machine.hasNext()) {
            count++
            machine.next()
        }
        assertEquals(defaultArgs.resolve().numTests, count)
    }

    @Test
    fun `generated test complexity scales correctly`() {
        val machine = StateMachine(defaultArgs.resolve())
        machine.notifyCasesExhausted(CaseKind.EDGE)
        machine.notifyCasesExhausted(CaseKind.SIMPLE)
        val generatedTests = machine.asSequence()
            .toList()
            .filterIsInstance<TestKind.Generated>()

        assertFalse(machine.hasNext(), "machine should be exhausted")

        assertTrue(generatedTests.any { it.complexity == 0 }, "At least one test should have complexity 0")
        assertTrue(
            generatedTests.any { it.complexity == defaultArgs.resolve().maxComplexity!! },
            "At least one test should have the max complexity"
        )
        // foldRight apparently doesn't guarantee order?
        var prevComplexity: Int = 0
        for (tk in generatedTests) {
            assertTrue(
                tk.complexity >= prevComplexity,
                "complexity should be monotonic increasing (${tk.complexity},$prevComplexity)"
            )
            prevComplexity = tk.complexity
        }
    }

    @Test
    fun `regression test complexity scales correctly`() {
        val machine = StateMachine(defaultArgs.resolve())
        val generatedTests = machine.asSequence()
            .toList()
            .filterIsInstance<TestKind.Regression>()

        assertFalse(machine.hasNext(), "machine should be exhausted")

        assertTrue(generatedTests.any { it.complexity == 0 }, "At least one test should have complexity 0")
        assertTrue(
            generatedTests.any { it.complexity == defaultArgs.resolve().maxComplexity!! },
            "At least one test should have the max complexity"
        )
        // foldRight apparently doesn't guarantee order?
        var prevComplexity: Int = 0
        for (tk in generatedTests) {
            assertTrue(
                tk.complexity >= prevComplexity,
                "complexity should be monotonic increasing (${tk.complexity},$prevComplexity)"
            )
            prevComplexity = tk.complexity
        }
    }

    @Test
    fun `machine runs correct number of regression tests`() {
        val machine = StateMachine(defaultArgs.resolve())
        val numRegressionTests = machine.asSequence().count { it is TestKind.Regression }
        assertEquals(defaultArgs.resolve().numRegressionTests!!, numRegressionTests)
    }

    @Test
    fun `cases are cut off by notifications`() {
        val machine = StateMachine(defaultArgs)

        (0 until 30).map { machine.next() }.forEach {
            assertTrue(it is TestKind.EdgeCase || it is TestKind.Regression)
        }
        machine.notifyCasesExhausted(CaseKind.EDGE)

        (0 until 30).map { machine.next() }.forEach {
            assertTrue(it is TestKind.SimpleCase || it is TestKind.Regression)
        }
        machine.notifyCasesExhausted(CaseKind.SIMPLE)

        machine.asSequence().forEach {
            assertTrue(it !is TestKind.EdgeCase && it !is TestKind.SimpleCase)
        }
    }
}