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
    private val sCMInterfaceRefOne: String = "examples.classdesign.superclassmismatch.interfaces.reference.One"
    private val sCMInterfaceRefNone: String = "examples.classdesign.superclassmismatch.interfaces.reference.None"
    private val sCMInterfaceRefMultiple: String =
        "examples.classdesign.superclassmismatch.interfaces.reference.Multiple"
    private val sCMInterfaceAttOne: String = "examples.classdesign.superclassmismatch.interfaces.One"
    private val sCMInterfaceAttNone: String = "examples.classdesign.superclassmismatch.interfaces.None"
    private val sCMInterfaceAttMultiple: String = "examples.classdesign.superclassmismatch.interfaces.Multiple"
    private val tPRefET: String = "examples.classdesign.typeparams.reference.ET"
    private val tPRefT: String = "examples.classdesign.typeparams.reference.T"
    private val tPAttET: String = "examples.classdesign.typeparams.ET"
    private val tpAttTE: String = "examples.classdesign.typeparams.TE"
    private val tpAttT: String  = "examples.classdesign.typeparams.T"

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
    fun testTypeParamMatch() {
        val analyzerMatch1 = analyzer(tPRefET, tPAttET)
        analyzerMatch1.typeParamsMatch()

        val analyzerMatch2 = analyzer(tPRefT, tpAttT)
        analyzerMatch2.typeParamsMatch()
    }

    @Test
    fun testTypeParamsMismatchOrder() {
        val analyzer = analyzer(tPRefET, tpAttTE)
        analyzer.assertMismatchMsg(
            """
                Expected type parameters : <E, T>
                Found type parameters    : <T, E>
            """.trimIndent(),
            name = false
        )
    }

    @Test
    fun testTypeParamsMismatchMissing() {
        val analyzer = analyzer(tPRefET, tpAttT)
        analyzer.assertMismatchMsg(
            """
                Expected type parameters : <E, T>
                Found type parameter     : <T>
            """.trimIndent(),
            name = false
        )
    }

    @Test
    fun testTypeParamsMismatchExtra() {
        val analyzer = analyzer (tPRefT, tpAttTE)
        analyzer.assertMismatchMsg(
            """
                Expected type parameter : <T>
                Found type parameters   : <T, E>
            """.trimIndent(),
            name = false
        )
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
        val analyzer = analyzer(sCMInterfaceRefNone, sCMInterfaceAttNone)
        analyzer.runSuite(name = false)
    }

    @Test
    fun testSuperClassMismatchInterfacesNoneOne() {
        val analyzer = analyzer(sCMInterfaceRefNone, sCMInterfaceAttOne)
        analyzer.assertMismatchMsg(
            "Expected class to not implement any interfaces, but class implemented `java.util.List'.",
            name = false
        )
    }

    @Test
    fun testSuperClassMismatchInterfacesNoneMultiple() {
        val analyzer = analyzer(sCMInterfaceRefNone, sCMInterfaceAttMultiple)
        analyzer.assertMismatchMsg(
            "Expected class to not implement any interfaces, " +
                    "but class implemented `java.util.Iterator', `java.util.function.Function', and `java.util.List'.",
            name = false
        )
    }

    @Test
    fun testSuperClassMismatchInterfacesOneNone() {
        val analyzer = analyzer(sCMInterfaceRefOne, sCMInterfaceAttNone)
        analyzer.assertMismatchMsg(
            "Expected class to implement `java.util.Set', but class did not implement any interfaces.",
            name = false
        )
    }

    @Test
    fun testSuperClassMismatchInterfacesOneOne() {
        val analyzer = analyzer(sCMInterfaceRefOne, sCMInterfaceAttOne)
        analyzer.assertMismatchMsg(
            "Expected class to implement `java.util.Set', but class implemented `java.util.List'.",
            name = false
        )
    }
    
    @Test
    fun testSuperClassMismatchInterfacesOneMultiple() {
        val analyzer = analyzer(sCMInterfaceRefOne, sCMInterfaceAttMultiple)
        analyzer.assertMismatchMsg(
            "Expected class to implement `java.util.Set', " +
                    "but class implemented `java.util.Iterator', `java.util.function.Function', and `java.util.List'.",
            name = false
        )
    }
    
    @Test
    fun testSuperClassMismatchInterfacesMultipleNone() {
        val analyzer = analyzer(sCMInterfaceRefMultiple, sCMInterfaceAttNone)
        analyzer.assertMismatchMsg(
            "Expected class to implement `java.lang.Iterable', `java.util.function.Function', and `java.util.List', " +
                    "but class did not implement any interfaces.",
            name = false
        )
    }

    @Test
    fun testSuperClassMismatchInterfacesMultipleOne() {
        val analyzer = analyzer(sCMInterfaceRefMultiple, sCMInterfaceAttOne)
        analyzer.assertMismatchMsg(
            "Expected class to implement `java.lang.Iterable', `java.util.function.Function', and `java.util.List', " +
                    "but class implemented `java.util.List'.",
            name = false
        )
    }

    @Test
    fun testSuperClassMismatchInterfacesMultipleMultiple() {
        val analyzer = analyzer(sCMInterfaceRefMultiple, sCMInterfaceAttMultiple)
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
        typeParams: Boolean = true,
        superClasses: Boolean = true,
        fields: Boolean = true,
        methods: Boolean = true
    ) {
        try {
            runSuite(name, classStatus, typeParams, superClasses, fields, methods)
        } catch (e: ClassDesignMismatchException) {
            assertEquals(expected, e.msg)
        } catch (other: Exception) {
            println("Suite threw the wrong type of exception:")
            throw other
        }
    }
}