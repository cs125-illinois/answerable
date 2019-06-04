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
    private val modifierMismatchPrefix: String = "examples.classdesign.modifiers"
    private val tPRefET: String = "examples.classdesign.typeparams.reference.ET"
    private val tPRefT: String = "examples.classdesign.typeparams.reference.T"
    private val tPAttET: String = "examples.classdesign.typeparams.ET"
    private val tpAttTE: String = "examples.classdesign.typeparams.TE"
    private val tpAttT: String = "examples.classdesign.typeparams.T"
    private val fieldMismatchSimpleRef: String = "examples.classdesign.publicapi.fields.reference.Simple"
    private val fieldMismatchPrefix: String = "examples.classdesign.publicapi.fields"
    private val methodMismatchPrefix: String = "examples.classdesign.publicapi.methods"
    private val methodMismatchSimpleRef: String = "$methodMismatchPrefix.reference.Simple"
    private val methodMismatchTPRef: String = "$methodMismatchPrefix.reference.TypeParam"
    private val methodMismatchConsRef: String = "$methodMismatchPrefix.reference.Constructor"
    private val methodMismatchThrowsRef: String = "$methodMismatchPrefix.reference.Throws"

    @Test
    fun testClassDesignCorrect1() {
        val analyzer = analyzer(classDesignCorrectReference1, classDesignCorrectAttempt1)
        assertTrue(analyzer.runSuite().all { it.result is Matched })
    }

    @Test
    fun testClassDesignCorrect2() {
        val analyzer = analyzer(classDesignCorrectReference2, classDesignCorrectAttempt2)
        analyzer.runSuite(methods = false)
        assertTrue(analyzer.publicMethodsMatch().result is Matched) // should ignore the @Next method.
    }

    @Test
    fun testStatusMismatch() {
        var analyzer = analyzer(statusMismatchInterface, statusMismatchClass)
        analyzer.assertMismatchMsg("Class status mismatch!\n" +
                "Expected : an interface\n" +
                "Found    : a class")

        analyzer = analyzer(statusMismatchClass, statusMismatchInterface)
        analyzer.assertMismatchMsg("Class status mismatch!\n" +
                "Expected : a class\n" +
                "Found    : an interface")

        analyzer = analyzer(statusMismatchInterface, statusMismatchInterface)
        assertTrue(analyzer.runSuite().all { it.result is Matched })
        analyzer = analyzer(statusMismatchClass, statusMismatchClass)
        assertTrue(analyzer.runSuite().all { it.result is Matched })
    }

    @Test
    fun testModifierMatch() {
        val analyzer = analyzer("$modifierMismatchPrefix.reference.Final", "$modifierMismatchPrefix.Final")

        assertTrue(analyzer.runSuite().all { it.result is Matched })
    }

    @Test
    fun testModifierMismatch() {
        val analyzer = analyzer("$modifierMismatchPrefix.reference.Final", "$modifierMismatchPrefix.Abstract")

        analyzer.assertMismatchMsg(
            """
                Class modifiers mismatch!
                Expected : public final
                Found    : public abstract
            """.trimIndent(),
            name = false
        )
    }

    @Test
    fun testTypeParamMatch() {
        val analyzerMatch1 = analyzer(tPRefET, tPAttET)
        assertTrue(analyzerMatch1.typeParamsMatch().result is Matched)

        val analyzerMatch2 = analyzer(tPRefT, tpAttT)
        assertTrue(analyzerMatch2.typeParamsMatch().result is Matched)
    }

    // Not checking names for these tests allows re-use
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
        val analyzer = analyzer(tPRefT, tpAttTE)
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
        assertTrue(analyzer.runSuite(name = false).all { it.result is Matched })
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

    @Test
    fun testFieldMatchSimple() {
        val analyzer = analyzer(fieldMismatchSimpleRef, "$fieldMismatchPrefix.Simple")
        assertTrue(analyzer.runSuite().all { it.result is Matched })
    }

    @Test
    fun testFieldMatchTypeParam() {
        val analyzer = analyzer("$fieldMismatchPrefix.reference.TypeParam", "$fieldMismatchPrefix.TypeParam")
        assertTrue(analyzer.runSuite().all { it.result is Matched })
    }

    @Test
    fun testFieldMismatchMissingInt() {
        val analyzer = analyzer(fieldMismatchSimpleRef, "$fieldMismatchPrefix.String")
        analyzer.assertMismatchMsg(
            """
                |Expected another public field:
                |  public static int a
            """.trimMargin(),
            name = false
        )
    }

    @Test
    fun testFieldMismatchMissingString() {
        val analyzer = analyzer(fieldMismatchSimpleRef, "$fieldMismatchPrefix.StaticInt")
        analyzer.assertMismatchMsg(
            """
                |Expected another public field:
                |  public String s
            """.trimMargin(),
            name = false
        )
    }

    @Test
    fun testFieldMismatchBadNames() {
        val analyzer = analyzer(fieldMismatchSimpleRef, "$fieldMismatchPrefix.BadNames")
        analyzer.assertMismatchMsg(
            """
                |Expected your class to have public fields:
                |  public String s
                |  public static int a
                |but found public fields:
                |  public String myString
                |  public static int myInt
            """.trimMargin(),
            name = false
        )
    }

    @Test
    fun testFieldMismatchOneTooMany() {
        val analyzer = analyzer(fieldMismatchSimpleRef, "$fieldMismatchPrefix.OneTooMany")
        analyzer.assertMismatchMsg(
            """
                |Found an unexpected public field:
                |  public boolean extra
            """.trimMargin(),
            name = false
        )
    }

    @Test
    fun testFieldMismatchMultipleTooMany() {
        val analyzer = analyzer(fieldMismatchSimpleRef, "$fieldMismatchPrefix.MultipleTooMany")
        analyzer.assertMismatchMsg(
            """
                |Found unexpected public fields:
                |  public boolean extra1
                |  public byte extra2
            """.trimMargin(),
            name = false
        )
    }

    @Test
    fun testFieldMismatchWrongModifiers() {
        val analyzer = analyzer(fieldMismatchSimpleRef, "$fieldMismatchPrefix.WrongModifiers")
        analyzer.assertMismatchMsg(
            """
                |Expected your class to have public fields:
                |  public String s
                |  public static int a
                |but found public fields:
                |  public int a
                |  public static String s
            """.trimMargin(),
            name = false
        )
    }

    @Test
    fun testPublicMethodsMatchSimple() {
        val analyzer = analyzer(methodMismatchSimpleRef, "$methodMismatchPrefix.Simple")
        assertTrue(analyzer.runSuite().all { it.result is Matched })
    }

    @Test
    fun testPublicMethodsMatchTypeParam() {
        val analyzer = analyzer(methodMismatchTPRef, "$methodMismatchPrefix.TypeParam")
        assertTrue(analyzer.runSuite().all { it.result is Matched })
    }

    @Test
    fun testPublicMethodsMatchConstructor() {
        val analyzer = analyzer(methodMismatchConsRef, "$methodMismatchPrefix.Constructor")
        assertTrue(analyzer.runSuite().all { it.result is Matched })
    }

    @Test
    fun testPublicMethodsMatchThrows() {
        val analyzer = analyzer(methodMismatchThrowsRef, "$methodMismatchPrefix.Throws")
        assertTrue(analyzer.runSuite().all { it.result is Matched })
    }

    // mkPublicApiMismatchMsg is fairly thoroughly tested by the Field tests, so here we want to focus on
    // testing components of the Method matcher above testing the error messages exhaustively again.
    // Thus constructor mismatches are probably going to appear in nearly every test.
    @Test
    fun testPublicMethodMismatchSimpleVTypeParam() {
        val analyzer = analyzer(methodMismatchSimpleRef, methodMismatchTPRef)
        analyzer.assertMismatchMsg(
            """
                |Expected your class to have public methods:
                |  public Simple()
                |  public String toString()
                |  public boolean lt(int, int)
                |  public static int zero()
                |but found public methods:
                |  public TypeParam()
                |  public final <T> T foo(String, T)
            """.trimMargin(),
            name = false
        )
    }

    @Test
    fun testPublicMethodMismatchSimpleVCons() {
        val analyzer = analyzer(methodMismatchConsRef, methodMismatchSimpleRef)
        analyzer.assertMismatchMsg(
            """
                |Expected your class to have public method:
                |  public Constructor(int, boolean)
                |but found public methods:
                |  public Simple()
                |  public String toString()
                |  public boolean lt(int, int)
                |  public static int zero()
            """.trimMargin(),
            name = false
        )
    }

    @Test
    fun testPublicMethodMismatchMissingFinal() {
        val analyzer = analyzer(methodMismatchTPRef, "$methodMismatchPrefix.MissingFinal")
        analyzer.assertMismatchMsg(
            """
                |Expected your class to have public methods:
                |  public TypeParam()
                |  public final <T> T foo(String, T)
                |but found public methods:
                |  public <T> T foo(String, T)
                |  public MissingFinal()
            """.trimMargin(),
            name = false
        )
    }

    @Test
    fun testPublicMethodMismatchMissingThrow() {
        val analyzer = analyzer(methodMismatchThrowsRef, "$methodMismatchPrefix.MissingThrow")
        analyzer.assertMismatchMsg(
            """
                |Expected your class to have public methods:
                |  public Throws()
                |  public void foo() throws IllegalStateException, StackOverflowError
                |but found public methods:
                |  public MissingThrow()
                |  public void foo() throws IllegalStateException
            """.trimMargin(),
            name = false
        )
    }

    private fun analyzer(refName: String, attName: String): ClassDesignAnalysis =
        ClassDesignAnalysis(getSolutionClass(refName), getAttemptClass(attName))

    private fun ClassDesignAnalysis.assertMismatchMsg(
        expected: String,
        name: Boolean = true,
        classStatus: Boolean = true,
        classModifiers: Boolean = true,
        typeParams: Boolean = true,
        superClasses: Boolean = true,
        fields: Boolean = true,
        methods: Boolean = true
    ) = assertEquals(
        expected,
        runSuite(name, classStatus, classModifiers, typeParams, superClasses, fields, methods)
            .first { it.result is Mismatched }
            .toErrorMsg()
    )
}