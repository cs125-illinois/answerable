package edu.illinois.cs.cs125.answerable.jeedrunner

import edu.illinois.cs.cs125.answerable.AnswerableMisuseException
import edu.illinois.cs.cs125.answerable.ExecutedTestStep
import java.lang.Exception
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class TestService {

    private lateinit var answerable: Answerable

    @BeforeEach
    fun setup() {
        answerable = Answerable()
    }

    @Test
    fun testJavaCodeQuestionCodeAnswer() {
        answerable.loadNewQuestion("WidgetGetter", QuestionLanguage.JAVA,
            WIDGET_JAVA_REFERENCE_CODE, "Widget")
        val runner = answerable.submit("WidgetGetter", WIDGET_JAVA_SUBMISSION_CODE)
        runner.runTests().assertAllSucceeded()
    }

    @Test
    fun testJavaCodeQuestionCodeIncorrectAnswer() {
        answerable.loadNewQuestion("WidgetGetter", QuestionLanguage.JAVA,
            WIDGET_JAVA_REFERENCE_CODE, "Widget")
        val result = answerable.submit(
            "WidgetGetter",
            WIDGET_JAVA_SUBMISSION_CODE.replace("return ", "return 2 * ")
        ).runTests()
        assertTrue(result.testSteps.filterIsInstance<ExecutedTestStep>().any { !it.succeeded })
    }

    @Test
    fun testClassQuestionJavaCodeAnswer() {
        answerable.loadNewQuestion("WidgetGetter", examples.reference.Widget::class.java,
            language = QuestionLanguage.JAVA)
        val runner = answerable.submit("WidgetGetter", WIDGET_JAVA_SUBMISSION_CODE)
        runner.runTests().assertAllSucceeded()
    }

    @Test
    fun testSpecifyLanguageLate() {
        answerable.loadNewQuestion("WidgetGetter", examples.reference.Widget::class.java)
        val runner = answerable.submit("WidgetGetter", WIDGET_JAVA_SUBMISSION_CODE,
            overrideLanguage = QuestionLanguage.JAVA)
        runner.runTests().assertAllSucceeded()
    }

    @Test
    fun testJavaCodeQuestionClassAnswer() {
        answerable.loadNewQuestion("WidgetGetter", QuestionLanguage.JAVA,
            WIDGET_JAVA_REFERENCE_CODE, "Widget")
        val runner = answerable.submit("WidgetGetter", examples.Widget::class.java)
        runner.runTests().assertAllSucceeded()
    }

    @Test
    fun testJavaCodeQuestionClassIncorrectAnswer() {
        answerable.loadNewQuestion("WidgetGetter", QuestionLanguage.JAVA,
            WIDGET_JAVA_REFERENCE_CODE, "Widget")
        val result = answerable.submit("WidgetGetter", examples.wrong.Widget::class.java).runTests()
        assertTrue(result.testSteps.filterIsInstance<ExecutedTestStep>().any { !it.succeeded })
    }

    @Test
    fun testJavaClassQuestionClassAnswer() {
        answerable.loadNewQuestion("WidgetGetter", examples.reference.Widget::class.java)
        val result = answerable.submitAndTest("WidgetGetter", examples.Widget::class.java)
        result.assertAllSucceeded()
    }

    @Test
    fun testJavaCommonCode() {
        answerable.loadNewQuestion("WidgetGetter", QuestionLanguage.JAVA,
            WIDGET_JAVA_REFERENCE_CODE
                .replace("= setSprings", "= Adder.oneMore(setSprings)")
                .replace("return springs", "return Subtracter.oneLess(springs)"),
            "Widget", commonCode = listOf(ADDER_JAVA_COMMON_CODE, SUBTRACTER_JAVA_COMMON_CODE))
        val runner = answerable.submit("WidgetGetter",
            WIDGET_JAVA_SUBMISSION_CODE
                .replace("numSprings;", "Adder.oneMore(Subtracter.oneLess(numSprings));"))
        runner.runTests().assertAllSucceeded()
    }

    @Test
    fun testKotlinCodeQuestionCodeAnswer() {
        answerable.loadNewQuestion("Accumulate", QuestionLanguage.KOTLIN,
            ACCUMULATOR_KOTLIN_REFERENCE_CODE, "Accumulator")
        val runner = answerable.submit("Accumulate", ACCUMULATOR_KOTLIN_SUBMISSION_CODE)
        runner.runTests().assertAllSucceeded()
    }

    @Test
    fun testKotlinClassQuestionCodeAnswer() {
        answerable.loadNewQuestion("Accumulate", examples.reference.Accumulator::class.java)
        val runner = answerable.submit("Accumulate", ACCUMULATOR_KOTLIN_SUBMISSION_CODE, QuestionLanguage.KOTLIN)
        runner.runTests().assertAllSucceeded()
    }

    @Test
    fun testKotlinCodeQuestionClassAnswer() {
        answerable.loadNewQuestion("Accumulate", QuestionLanguage.KOTLIN,
            ACCUMULATOR_KOTLIN_REFERENCE_CODE, "Accumulator")
        val runner = answerable.submit("Accumulate", examples.Accumulator::class.java)
        runner.runTests().assertAllSucceeded()
    }

    @Test
    fun testKotlinClassQuestionClassAnswer() {
        answerable.loadNewQuestion("Accumulate", examples.reference.Accumulator::class.java)
        val runner = answerable.submit("Accumulate", examples.Accumulator::class.java)
        runner.runTests().assertAllSucceeded()
    }

    @Test
    fun testKotlinCommonCode() {
        val commonCode = """
            object Utils {
                const val theNumberZero = 0
                fun identity(value: Int) = value
            }
        """.trimIndent()
        answerable.loadNewQuestion("Accumulate", QuestionLanguage.KOTLIN,
            ACCUMULATOR_KOTLIN_REFERENCE_CODE.replace("= value", "= value + Utils.theNumberZero"),
            "Accumulator", commonCode = listOf(commonCode))
        val runner = answerable.submit("Accumulate",
            ACCUMULATOR_KOTLIN_SUBMISSION_CODE.replace("+= more", "+= Utils.identity(more)"))
        runner.runTests().assertAllSucceeded()
    }

    @Test
    fun testQuestionUnload() {
        assertFalse(answerable.unloadQuestion("WidgetGetter"))
        answerable.loadNewQuestion("WidgetGetter", QuestionLanguage.JAVA,
            WIDGET_JAVA_REFERENCE_CODE, "Widget")
        assertTrue(answerable.unloadQuestion("WidgetGetter"))
        val noQuestionError = assertThrows<Exception> {
            answerable.submitAndTest("WidgetGetter", WIDGET_JAVA_SUBMISSION_CODE)
        }
        assertEquals("No question named WidgetGetter is currently loaded", noQuestionError.message)
    }

    @Test
    fun testAnswerableMisuseExceptionUpdate() {
        val refCode = """
            import edu.illinois.cs.cs125.answerable.annotations.Solution;
            public class Widget {
                private int springs;
                public Widget(int setSprings) {
                    springs = setSprings;
                }
                @Solution
                public int getSprings() {
                    return springs;
                }
            }
        """.trimIndent()
        val errMsg: String = assertThrows<AnswerableMisuseException> {
            answerable.loadNewQuestion(
                "MissingGenerator",
                QuestionLanguage.JAVA,
                refCode,
                "Widget"
            )
        }.message!!
        assertEquals(
            "\nThe reference solution must provide either an @Generator or an @Next method if @Solution " +
                "is not static and no zero-argument constructor is accessible.\n" +
                "While trying to load new question: MissingGenerator.",
            errMsg
        )
    }

    companion object {
        @JvmField val WIDGET_JAVA_REFERENCE_CODE = """
            import edu.illinois.cs.cs125.answerable.annotations.*;
            import java.util.Random;
            public class Widget {
                private int springs;
                public Widget(int setSprings) {
                    springs = setSprings;
                }
                @Solution
                public int getSprings() {
                    return springs;
                }
                @Generator
                public static Widget generate(int complexity, Random random) {
                    return new Widget(complexity);
                }
            }
        """.trimIndent()
        @JvmField val WIDGET_JAVA_SUBMISSION_CODE = """
            public class Widget {
                private int numSprings = 0;
                public Widget(int mySprings) {
                    numSprings = mySprings;
                }
                public int getSprings() {
                    return numSprings;
                }
            }
        """.trimIndent()
        @JvmField val ADDER_JAVA_COMMON_CODE = """
            public class Adder {
                public static int oneMore(int value) {
                    return value + 1;
                }
            }
        """.trimIndent()
        @JvmField val SUBTRACTER_JAVA_COMMON_CODE = """
            public class Subtracter {
                public static int oneLess(int value) {
                    return value - 1;
                }
            }
        """.trimIndent()
        @JvmField val ACCUMULATOR_KOTLIN_REFERENCE_CODE = """
            import edu.illinois.cs.cs125.answerable.annotations.*
            import edu.illinois.cs.cs125.answerable.api.TestOutput
            import edu.illinois.cs.cs125.answerable.api.defaultIntGenerator
            import org.junit.jupiter.api.Assertions.assertEquals
            import java.util.Random
            class Accumulator(private var value: Int) {
                @Solution
                fun add(extra: Int) {
                    value += extra
                }
                val current: Int
                    get() = value
            }
            @Generator
            fun generate(complexity: Int, random: Random): Accumulator {
                return Accumulator(defaultIntGenerator(complexity, random))
            }
            @Verify
            fun verify(ours: TestOutput<Accumulator>, theirs: TestOutput<Accumulator>) {
                assertEquals(ours.receiver!!.current, theirs.receiver!!.current)
            }
        """.trimIndent()
        @JvmField val ACCUMULATOR_KOTLIN_SUBMISSION_CODE = """
            class Accumulator(private val initialValue: Int) {
                private var currentValue = initialValue
                fun add(more: Int) {
                    currentValue += more
                }
                val current: Int
                    get() = currentValue
            }
        """.trimIndent()
    }
}
