package edu.illinois.cs.cs125.answerable

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.lang.Exception
import java.lang.IllegalStateException
import java.lang.reflect.Field

internal class DiscovererTest {
    private val correctAdderReference: String = "examples.adder.correct1.reference.Adder"
    private val correctAdderAttempt: String = "examples.adder.correct1.Adder"
    private val correctPrinterReference: String = "examples.printer.correct1.reference.Printer"
    private val correctClassDesignReference: String = "examples.classdesign.correct1.reference.ClassDesign"

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

        val ref2 = getSolutionClass(correctClassDesignReference)
        assertTrue(ref2.isClassDesignReference())
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

    @Test
    fun testIsStaticVoid() {
        try {
            val printerReferenceClass = getSolutionClass(correctPrinterReference)
            val printerReferenceMethod = printerReferenceClass.getReferenceSolutionMethod()
            assertTrue(printerReferenceMethod.isStaticVoid())
        } catch (unused: Exception) {
            assertTrue(false)
        }
    }

    @Test
    fun testGetPublicFields() {
        try {
            val classDesignReferenceClass = getSolutionClass(correctClassDesignReference)
            assertEquals(
                "[public static int examples.classdesign.correct1.reference.ClassDesign.numGets]",
                classDesignReferenceClass.getPublicFields().toString()
            )
        } catch (unused: Exception) {
            assertTrue(false)
        }
    }

    @Test
    fun testGetPublicMethods() {
        try {
            val classDesignReferenceClass = getSolutionClass(correctClassDesignReference)
            assertEquals(
                "[public java.lang.Object examples.classdesign.correct1.reference.ClassDesign.get(int)]",
                classDesignReferenceClass.getPublicMethods(isReference = true).toString()
            )

            assertEquals(
                "[public java.lang.Object examples.classdesign.correct1.reference.ClassDesign.get(int), public static examples.classdesign.correct1.reference.ClassDesign examples.classdesign.correct1.reference.ClassDesign.next]",
                classDesignReferenceClass.getPublicMethods(isReference = false).toString()
            )
        } catch (unused: Exception) {
            assertTrue(false)
        }
    }
}