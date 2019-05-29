package edu.illinois.cs.cs125.answerable

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class ClassDesignAnalysisTest {
    private val classDesignCorrectReference1: String = "examples.classdesign.correct1.reference.ClassDesign"
    private val classDesignCorrectAttempt1: String = "examples.classdesign.correct1.ClassDesign"
    private val classDesignCorrectReference2: String = "examples.classdesign.correct2.reference.IterableList"
    private val classDesignCorrectAttempt2: String = "examples.classdesign.correct2.IterableList"
    private val statusMismatchInterface: String = "examples.classdesign.statusmismatch.reference.Question"
    private val statusMismatchClass: String = "examples.classdesign.statusmismatch.Question"
    private val superClassMismatchOnlyExtRef: String =
        "examples.classdesign.superclassmismatch.classes.reference.OnlyExt"
    private val superClassMismatchOnlyImplRef: String =
        "examples.classdesign.superclassmismatch.classes.reference.OnlyImpl"
    private val SCMInterfaceRefOne: String = "examples.classdesign.superclassmismatch.interfaces.reference.One"
    private val SCMInterfaceRefNone: String = "examples.classdesign.superclassmismatch.interfaces.reference.None"
    private val SCMInterfaceRefMultiple: String =
        "examples.classdesign.superclassmismatch.interfaces.reference.Multiple"
    private val SCMInterfaceAttOne: String = "examples.classdesign.superclassmismatch.interfaces.One"
    private val SCMInterfaceAttNone: String = "examples.classdesign.superclassmismatch.interfaces.None"
    private val SCMInterfaceAttMultiple: String = "examples.classdesign.superclassmismatch.interfaces.Multiple"

    @Test
    fun testClassDesignCorrect1() {
        val analyzer = analyzer(classDesignCorrectReference1, classDesignCorrectAttempt1)
        analyzer.runSuite()
    }

    @Test
    fun testClassDesignCorrect2() {
        val analyzer = analyzer(classDesignCorrectReference2, classDesignCorrectAttempt2)
        analyzer.runSuite(methods = false)
        assertTrue(analyzer.publicMethodsMatch()) // should ignore the @Next method.
    }

    @Test
    fun testStatusMismatch() {
        var analyzer = analyzer(statusMismatchInterface, statusMismatchClass)
        analyzer.assertMismatchMsg("Expected an interface but found a class.")

        analyzer = analyzer(statusMismatchClass, statusMismatchInterface)
        analyzer.assertMismatchMsg("Expected a class but found an interface.")

        analyzer = analyzer(statusMismatchInterface, statusMismatchInterface)
        analyzer.runSuite()
        analyzer = analyzer(statusMismatchClass, statusMismatchClass)
        analyzer.runSuite()
    }

    @Test
    fun testSuperClassMismatchExtNoExt() {
        // Not checking name so should be safe to test against multiple classes here.
        val analyzer = analyzer(
            superClassMismatchOnlyExtRef,
            "examples.classdesign.superclassmismatch.classes.OnlyExtNone"
        )
        analyzer.assertMismatchMsg(
            "Expected class to extend `java.util.ArrayList', but class did not extend any classes.",
            name = false // don't check names
        )
    }

    @Test
    fun testSuperClassMismatchExtWrongExt() {
        val analyzer = analyzer(
            superClassMismatchOnlyExtRef,
            "examples.classdesign.superclassmismatch.classes.OnlyExtWrong"
        )
        analyzer.assertMismatchMsg(
            "Expected class to extend `java.util.ArrayList', but class extended `java.util.LinkedList'.",
            name = false
        )
    }

    @Test
    fun testSuperClassMismatchExtImpl() {
        val analyzer = analyzer(
            superClassMismatchOnlyExtRef,
            "examples.classdesign.superclassmismatch.classes.OnlyExtImpl"
        )
        analyzer.assertMismatchMsg(
            "Expected class to not implement any interfaces, but class implemented `java.util.List'.",
            name = false
        )
    }

    @Test
    fun testSuperClassMismatchExtMultipleImpl() {
        val analyzer = analyzer(
            superClassMismatchOnlyExtRef,
            "examples.classdesign.superclassmismatch.classes.OnlyExtMultipleImpl"
        )
        analyzer.assertMismatchMsg(
            "Expected class to not implement any interfaces," +
                    " but class implemented `java.util.Collection', and `java.util.List'.",
            name = false
        )
    }

    @Test
    fun testSuperClassMismatchOnlyImplExt() {
        val analyzer = analyzer(
            superClassMismatchOnlyImplRef,
            "examples.classdesign.superclassmismatch.classes.OnlyImplNone"
        )
        analyzer.assertMismatchMsg(
            "Expected class to implement `java.util.Collection', but class did not implement any interfaces.",
            name = false
        )
    }

    @Test
    fun testSuperClassMismatchOnlyImplWrong() {
        val analyzer = analyzer(
            superClassMismatchOnlyImplRef,
            "examples.classdesign.superclassmismatch.classes.OnlyImplWrong"
        )
        analyzer.assertMismatchMsg(
            "Expected class to implement `java.util.Collection', but class implemented `java.util.List'.",
            name = false
        )
    }

    @Test
    fun testSuperClassMismatchOnlyImplTooMany() {
        val analyzer = analyzer(
            superClassMismatchOnlyImplRef,
            "examples.classdesign.superclassmismatch.classes.OnlyImplTooMany"
        )
        analyzer.assertMismatchMsg(
            "Expected class to implement `java.util.Collection', but class implemented `java.util.Collection', and `java.util.List'.",
            name = false
        )
    }

    @Test
    fun testSuperClassMismatchExtAndImplWrong() {
        val analyzer = analyzer(
            "examples.classdesign.superclassmismatch.classes.reference.ExtAndImpl",
            "examples.classdesign.superclassmismatch.classes.ExtAndImplWrong"
        )
        analyzer.assertMismatchMsg(
            """
                Expected class to extend `java.util.LinkedList', but class extended `java.util.ArrayList';
                Also expected class to implement `java.util.List', and `java.util.Collection', but class implemented `java.util.Set'.
            """.trimIndent(),
            name = false
        )
    }

    @Test
    fun testSuperClassMatchInterfacesNoneNone() {
        val analyzer = analyzer(SCMInterfaceRefNone, SCMInterfaceAttNone)
        analyzer.runSuite(name = false)
    }

    @Test
    fun testSuperClassMismatchInterfacesNoneOne() {
        val analyzer = analyzer(SCMInterfaceRefNone, SCMInterfaceAttOne)
        analyzer.assertMismatchMsg(
            "Expected class to not implement any interfaces, but class implemented `java.util.List'.",
            name = false
        )
    }

    @Test
    fun testSuperClassMismatchInterfacesNoneMultiple() {
        val analyzer = analyzer(SCMInterfaceRefNone, SCMInterfaceAttMultiple)
        analyzer.assertMismatchMsg(
            "Expected class to not implement any interfaces, " +
                    "but class implemented `java.util.Iterator', `java.util.function.Function', and `java.util.List'.",
            name = false
        )
    }

    @Test
    fun testSuperClassMismatchInterfacesOneNone() {
        val analyzer = analyzer(SCMInterfaceRefOne, SCMInterfaceAttNone)
        analyzer.assertMismatchMsg(
            "Expected class to implement `java.util.Set', but class did not implement any interfaces.",
            name = false
        )
    }

    @Test
    fun testSuperClassMismatchInterfacesOneOne() {
        val analyzer = analyzer(SCMInterfaceRefOne, SCMInterfaceAttOne)
        analyzer.assertMismatchMsg(
            "Expected class to implement `java.util.Set', but class implemented `java.util.List'.",
            name = false
        )
    }
    
    @Test
    fun testSuperClassMismatchInterfacesOneMultiple() {
        val analyzer = analyzer(SCMInterfaceRefOne, SCMInterfaceAttMultiple)
        analyzer.assertMismatchMsg(
            "Expected class to implement `java.util.Set', " +
                    "but class implemented `java.util.Iterator', `java.util.function.Function', and `java.util.List'.",
            name = false
        )
    }
    
    @Test
    fun testSuperClassMismatchInterfacesMultipleNone() {
        val analyzer = analyzer(SCMInterfaceRefMultiple, SCMInterfaceAttNone)
        analyzer.assertMismatchMsg(
            "Expected class to implement `java.lang.Iterable', `java.util.function.Function', and `java.util.List', " +
                    "but class did not implement any interfaces.",
            name = false
        )
    }

    @Test
    fun testSuperClassMismatchInterfacesMultipleOne() {
        val analyzer = analyzer(SCMInterfaceRefMultiple, SCMInterfaceAttOne)
        analyzer.assertMismatchMsg(
            "Expected class to implement `java.lang.Iterable', `java.util.function.Function', and `java.util.List', " +
                    "but class implemented `java.util.List'.",
            name = false
        )
    }

    @Test
    fun testSuperClassMismatchInterfacesMultipleMultiple() {
        val analyzer = analyzer(SCMInterfaceRefMultiple, SCMInterfaceAttMultiple)
        analyzer.assertMismatchMsg(
            "Expected class to implement `java.lang.Iterable', `java.util.function.Function', and `java.util.List', " +
                    "but class implemented `java.util.Iterator', `java.util.function.Function', and `java.util.List'.",
            name = false
        )
    }

    private fun analyzer(refName: String, attName: String): ClassDesignAnalysis =
        ClassDesignAnalysis(getSolutionClass(refName), getAttemptClass(attName))

    private fun ClassDesignAnalysis.assertMismatchMsg(
        expected: String,
        name: Boolean = true,
        classStatus: Boolean = true,
        superClasses: Boolean = true,
        fields: Boolean = true,
        methods: Boolean = true
    ) {
        try {
            runSuite(name, classStatus, superClasses, fields, methods)
        } catch (e: ClassDesignMismatchException) {
            assertEquals(expected, e.msg)
        } catch (other: Exception) {
            println("Suite threw the wrong type of exception:")
            throw other
        }
    }
}