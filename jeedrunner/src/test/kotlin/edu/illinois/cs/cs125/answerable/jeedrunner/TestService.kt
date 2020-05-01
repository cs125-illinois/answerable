package edu.illinois.cs.cs125.answerable.jeedrunner

import edu.illinois.cs.cs125.answerable.AnswerableMisuseException
import edu.illinois.cs.cs125.answerable.ExecutedTestStep
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.Exception

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
    fun testJavaCommonCode() {
        val commonAdder = """
            public class Adder {
                public static int oneMore(int value) {
                    return value + 1;
                }
            }
        """.trimIndent()
        val commonSubtracter = """
            public class Subtracter {
                public static int oneLess(int value) {
                    return value - 1;
                }
            }
        """.trimIndent()
        answerable.loadNewQuestion("WidgetGetter", QuestionLanguage.JAVA,
            WIDGET_JAVA_REFERENCE_CODE
                .replace("= setSprings", "= Adder.oneMore(setSprings)")
                .replace("return springs", "return Subtracter.oneLess(springs)"),
            "Widget", commonCode = listOf(commonAdder, commonSubtracter))
        val runner = answerable.submit("WidgetGetter",
            WIDGET_JAVA_SUBMISSION_CODE
                .replace("numSprings;", "Adder.oneMore(Subtracter.oneLess(numSprings));"))
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
        val WIDGET_JAVA_REFERENCE_CODE = """
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
        val WIDGET_JAVA_SUBMISSION_CODE = """
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
    }

}
