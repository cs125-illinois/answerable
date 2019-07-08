package edu.illinois.cs.cs125.answerable

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

internal class DiscovererTest {
    private val correctAdderReference: String = "examples.adder.correct.reference.Adder"
    private val correctAdderAttempt: String = "examples.adder.correct.Adder"
    private val correctPrinterReference: String = "examples.printer.correct.reference.Printer"
    private val correctClassDesignReference: String = "examples.classdesign.correct1.reference.ClassDesign"

    @Test
    fun testGetSolutionClassAdder() {
        getSolutionClass(correctAdderReference)
    }

    @Test
    fun testGetAttemptClassAdder() {
        getAttemptClass(correctAdderAttempt)
    }

    @Test
    fun testGetReferenceSolutionMethod() {
        val soln = getSolutionClass(correctAdderReference)
        val ref = soln.getReferenceSolutionMethod()
        assertEquals("add", ref?.name)
    }

    @Test
    fun testGetSolutionAttemptMethod() {
        val soln = getSolutionClass(correctAdderReference)
        val solnMethod = soln.getReferenceSolutionMethod()

        val attempt = getAttemptClass(correctAdderAttempt)
        val attemptMethod = attempt.findSolutionAttemptMethod(solnMethod)

        assertEquals("add", attemptMethod?.name)
        assertEquals(Int::class.java, attemptMethod?.genericReturnType)
        assertArrayEquals(arrayOf(Int::class.java, Int::class.java), attemptMethod?.genericParameterTypes)
    }

    @Test
    fun testIsPrinter() {
        val printerReferenceClass = getSolutionClass(correctPrinterReference)
        val printerReferenceMethod = printerReferenceClass.getReferenceSolutionMethod()
        assertTrue(printerReferenceMethod?.isPrinter() ?: false)
    }

    @Test
    fun testGetPublicFields() {
        val classDesignReferenceClass = getSolutionClass(correctClassDesignReference)
        assertEquals(
            "[public static int examples.classdesign.correct1.reference.ClassDesign.numGets]",
            classDesignReferenceClass.getPublicFields().toString()
        )
    }

    @Test
    fun testGetPublicMethods() {
        val classDesignReferenceClass = getSolutionClass(correctClassDesignReference)

        assertEquals(
            "[public java.lang.Object examples.classdesign.correct1.reference.ClassDesign.get(int), public static examples.classdesign.correct1.reference.ClassDesign examples.classdesign.correct1.reference.ClassDesign.next(examples.classdesign.correct1.reference.ClassDesign,int)]",
            classDesignReferenceClass.getPublicMethods().toString()
        )
    }

    @Test
    fun testGetEnabledEdgeCases() {
        val reference = getSolutionClass(correctAdderReference)

        assertArrayEquals(intArrayOf(-1, 0, 1), reference.getEnabledEdgeCases(arrayOf())[Int::class.java]!!.array as IntArray)
    }
}