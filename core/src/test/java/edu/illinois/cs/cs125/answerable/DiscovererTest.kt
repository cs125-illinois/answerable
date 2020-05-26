package edu.illinois.cs.cs125.answerable

import edu.illinois.cs.cs125.answerable.annotations.getSolution
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class DiscovererTest {
    private val correctAdderReference: String = "examples.adder.correct.reference.Adder"
    private val correctAdderAttempt: String = "examples.adder.correct.Adder"
    private val correctPrinterReference: String = "examples.printer.correct.reference.Printer"
    private val correctClassDesignReference: String = "examples.classdesign.correct1.reference.ClassDesign"

    @Test
    fun testGetReferenceSolutionMethod() {
        val soln = findClass(correctAdderReference)
        val ref = soln.getSolution()
        assertEquals("add", ref?.name)
    }

    @Test
    fun testGetSolutionAttemptMethod() {
        val soln = findClass(correctAdderReference)
        val solnMethod = soln.getSolution()

        val attempt = findClass(correctAdderAttempt)
        val attemptMethod = attempt.findSolutionAttemptMethod(solnMethod, soln)

        assertEquals("add", attemptMethod?.name)
        assertEquals(Int::class.java, attemptMethod?.genericReturnType)
        assertArrayEquals(arrayOf(Int::class.java, Int::class.java), attemptMethod?.genericParameterTypes)
    }

    @Test
    fun testIsPrinter() {
        val printerReferenceClass = findClass(correctPrinterReference)
        val printerReferenceMethod = printerReferenceClass.getSolution()
        assertTrue(printerReferenceMethod?.isPrinter() ?: false)
    }

    @Test
    fun testGetPublicFields() {
        val classDesignReferenceClass = findClass(correctClassDesignReference)
        assertEquals(
            "[public static int examples.classdesign.correct1.reference.ClassDesign.numGets]",
            classDesignReferenceClass.publicFields.toString()
        )
    }

    @Test
    fun testGetPublicMethods() {
        val classDesignReferenceClass = findClass(correctClassDesignReference)

        assertEquals(
            "[public java.lang.Object examples.classdesign.correct1.reference.ClassDesign.get(int), " +
                "public static examples.classdesign.correct1.reference.ClassDesign " +
                "examples.classdesign.correct1.reference.ClassDesign.next(" +
                "examples.classdesign.correct1.reference.ClassDesign,int)]",
            classDesignReferenceClass.publicMethods.toString()
        )
    }

    @Test
    fun testGetEnabledEdgeCases() {
        val reference = findClass(correctAdderReference)

        assertArrayEquals(
            intArrayOf(-1, 0, 1),
            (reference.getEnabledEdgeCases(arrayOf())[Int::class.java] ?: error("")).array as IntArray
        )
    }
}
