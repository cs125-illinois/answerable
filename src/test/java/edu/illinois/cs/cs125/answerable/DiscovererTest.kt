package edu.illinois.cs.cs125.answerable

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.lang.Exception
import java.lang.IllegalStateException

internal class DiscovererTest {
    val correctAdderReference: String = "examples.AdderCorrect.Reference.Adder"
    val correctAdderAttempt: String = "examples.AdderCorrect.Adder"

    @Test
    fun testGetSolutionClassAdder() {
        try {
            getSolutionClass(correctAdderReference)
        } catch (e: IllegalStateException) {
            assertTrue(false)
        }
    }

    @Test
    fun testIsClassDesignReference() {
        val ref = getSolutionClass(correctAdderReference)
        assertFalse(ref.isClassDesignReference())
    }

    @Test
    fun testGetAttemptClassAdder() {
        try {
            getAttemptClass(correctAdderAttempt)
        } catch (e: IllegalStateException) {
            assertTrue(false)
        }
    }

    @Test
    fun testGetReferenceSolutionMethod() {
        try {
            val soln = getSolutionClass(correctAdderReference)
            val ref = soln.getReferenceSolutionMethod()
            assertEquals("add", ref.name)
        } catch (unused: Exception) {
            assertTrue(false)
        }
    }

    @Test
    fun testGetSolutionAttemptMethod() {
        try {
            val soln = getSolutionClass(correctAdderReference)
            val solnMethod = soln.getReferenceSolutionMethod()

            val attempt = getAttemptClass(correctAdderAttempt)
            val attemptMethod = attempt.findSolutionAttemptMethod(solnMethod)

            assertEquals("add", attemptMethod.name)
            assertEquals(Int::class.java, attemptMethod.genericReturnType)
            assertArrayEquals(arrayOf(Int::class.java, Int::class.java), attemptMethod.genericParameterTypes)
        } catch (unused: Exception) {
            assertTrue(false)
        }
    }
}