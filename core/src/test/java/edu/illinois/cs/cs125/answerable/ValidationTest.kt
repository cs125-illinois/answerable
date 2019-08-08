package edu.illinois.cs.cs125.answerable

import examples.testgeneration.validation.reference.ArgsOnInvalidVerify
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class ValidationTest {

    @Test
    fun testAccumulatingFailure() {
        val errMsg = assertThrows<AnswerableMisuseException> {
            TestGenerator(examples.testgeneration.validation.reference.AccumulatingFailure::class.java)
        }.message
        Assertions.assertEquals(
            "\n@Verify methods must take parameter types [TestOutput, TestOutput] and optionally a java.util.Random.\n" +
                    "While validating @Verify method `private static void verify()'.\n" +
                    "\n" +
                    "@DefaultTestRunArguments can only be applied to a @Solution or standalone @Verify method.\n" +
                    "While validating method `private static void verify()'.", errMsg
        )
    }

    @Test
    fun testArgsAnnotationOnVerifyError() {
        val errMsg = assertThrows<AnswerableMisuseException> {
            TestGenerator(ArgsOnInvalidVerify::class.java)
        }.message
        Assertions.assertEquals(
            "\n@DefaultTestRunArguments can only be applied to a @Solution or standalone @Verify method.\n" +
                    "While validating method `public static void verify(TestOutput<Void>, TestOutput<Void>)'.", errMsg
        )
    }
}