package edu.illinois.cs.cs125.answerable.jeedrunner;

import edu.illinois.cs.cs125.answerable.ExecutedTestStep;
import edu.illinois.cs.cs125.answerable.TestingResults;
import edu.illinois.cs.cs125.jeed.core.CompilationFailed;
import edu.illinois.cs.cs125.jeed.core.Sandbox;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

public class TestServiceJavaApi {

    private Answerable answerable;

    @BeforeEach
    public void setup() {
        answerable = new Answerable();
    }

    @Test
    public void testJavaCodeQuestionCodeAnswer() throws CompilationFailed {
        answerable.buildNewQuestion("WidgetGetter", QuestionLanguage.JAVA,
                TestService.WIDGET_JAVA_REFERENCE_CODE, "Widget").loadNewQuestion();
        answerable.buildSubmission("WidgetGetter", TestService.WIDGET_JAVA_SUBMISSION_CODE)
                .submit().runTests().assertAllSucceeded();
    }

    @Test
    public void testClassQuestionJavaCodeAnswer() throws CompilationFailed {
        answerable.buildNewQuestion("WidgetGetter", examples.reference.Widget.class)
                .language(QuestionLanguage.JAVA).loadNewQuestion();
        answerable.buildSubmission("WidgetGetter", TestService.WIDGET_JAVA_SUBMISSION_CODE)
                .submit().runTests().assertAllSucceeded();
    }

    @Test
    public void testSpecifyLanguageLate() throws CompilationFailed {
        answerable.buildNewQuestion("WidgetGetter", examples.reference.Widget.class).loadNewQuestion();
        answerable.buildSubmission("WidgetGetter", TestService.WIDGET_JAVA_SUBMISSION_CODE)
                .overrideLanguage(QuestionLanguage.JAVA).submit().runTests().assertAllSucceeded();
    }

    @Test
    public void testJavaCodeQuestionClassAnswer() throws CompilationFailed {
        answerable.buildNewQuestion("WidgetGetter", QuestionLanguage.JAVA,
                TestService.WIDGET_JAVA_REFERENCE_CODE, "Widget").loadNewQuestion();
        answerable.buildSubmission("WidgetGetter", examples.Widget.class)
                .submit().runTests().assertAllSucceeded();
    }

    @Test
    public void testJavaCommonCode() throws CompilationFailed {
        answerable.buildNewQuestion("WidgetGetter", QuestionLanguage.JAVA,
                TestService.WIDGET_JAVA_REFERENCE_CODE
                        .replace("= setSprings", "= Adder.oneMore(setSprings)")
                        .replace("return springs", "return Subtracter.oneLess(springs)"), "Widget")
                .commonCode(TestService.ADDER_JAVA_COMMON_CODE, TestService.SUBTRACTER_JAVA_COMMON_CODE)
                .loadNewQuestion();
        answerable.buildSubmission("WidgetGetter", TestService.WIDGET_JAVA_SUBMISSION_CODE
                        .replace("numSprings;", "Adder.oneMore(Subtracter.oneLess(numSprings));")
        ).submit().runTests().assertAllSucceeded();
    }

    @Test
    public void testKotlinCodeQuestionCodeAnswer() throws CompilationFailed {
        answerable.buildNewQuestion("Accumulate", QuestionLanguage.KOTLIN,
                TestService.ACCUMULATOR_KOTLIN_REFERENCE_CODE, "Accumulator").loadNewQuestion();
        answerable.buildSubmission("Accumulate", TestService.ACCUMULATOR_KOTLIN_SUBMISSION_CODE)
                .submit().runTests().assertAllSucceeded();
    }

    @Test
    public void testKotlinClassQuestionCodeAnswer() throws CompilationFailed {
        answerable.buildNewQuestion("Accumulate", examples.reference.Accumulator.class)
                .language(QuestionLanguage.KOTLIN).loadNewQuestion();
        answerable.buildSubmission("Accumulate", TestService.ACCUMULATOR_KOTLIN_SUBMISSION_CODE)
                .submit().runTests().assertAllSucceeded();
    }

    @Test
    public void testKotlinCodeQuestionClassAnswer() throws CompilationFailed {
        answerable.buildNewQuestion("Accumulate", QuestionLanguage.KOTLIN,
                TestService.ACCUMULATOR_KOTLIN_REFERENCE_CODE, "Accumulator").loadNewQuestion();
        answerable.buildSubmission("Accumulate", examples.Accumulator.class)
                .submit().runTests().assertAllSucceeded();
    }

    @Test
    public void testKotlinClassQuestionClassAnswer() {
        answerable.buildNewQuestion("Accumulate", examples.reference.Accumulator.class)
                .language(QuestionLanguage.KOTLIN).loadNewQuestion();
        answerable.buildSubmission("Accumulate", examples.Accumulator.class)
                .submit().runTests().assertAllSucceeded();
    }

    @Test
    public void testClassLoadingRestrictions() throws CompilationFailed {
        Sandbox.ClassLoaderConfiguration restriction = new Sandbox.ClassLoaderConfiguration(
                Collections.emptySet(), Collections.singleton("java.util."),
                Collections.emptySet(), Collections.emptySet(), false);
        answerable.buildNewQuestion("Sort", QuestionLanguage.JAVA, TestService.SORT_JAVA_REFERENCE_CODE, "Sorter")
            .classLoaderConfiguration(restriction).loadNewQuestion();
        TestingResults results = answerable.buildSubmission("Sort", TestService.SORT_JAVA_SUBMISSION_CODE_CHEATY)
            .submit().runTests();
        Assertions.assertTrue(results.getTestSteps().stream().filter(ts -> ts instanceof ExecutedTestStep)
                .anyMatch(ts -> !((ExecutedTestStep) ts).getSucceeded()));
        results = answerable.buildSubmission("Sort", TestService.SORT_JAVA_SUBMISSION_CODE_LEGITIMATE)
                .submit().runTests();
        results.assertAllSucceeded();
    }

}
