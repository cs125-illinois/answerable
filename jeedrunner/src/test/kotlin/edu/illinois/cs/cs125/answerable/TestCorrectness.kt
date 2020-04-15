package edu.illinois.cs.cs125.answerable

import org.junit.Assert
import org.junit.Test

class TestCorrectness {

    @Test
    fun testPrintOutputCorrect() {
        val result = testFromStrings(reference = """
            import edu.illinois.cs.cs125.answerable.api.*;
            public class Example {
                @Solution(prints = true)
                public static void sayHi() {
                    System.out.println("Hi!");
                }
            }
        """.trimIndent(), submission = """
            public class Example {
                public static void sayHi() {
                    System.out.print("Hi");
                    System.out.println("!");
                }
            }
        """.trimIndent(), className = "Example")
        result.testSteps.filterIsInstance<ExecutedTestStep>().forEach {
            if (!it.succeeded) {
                Assert.fail(it.toJson())
            }
        }
    }

    @Test
    fun testPrintOutputWrong() {
        val result = testFromStrings(reference = """
            import edu.illinois.cs.cs125.answerable.api.*;
            public class Example {
                @Solution(prints = true)
                public static void sayHi() {
                    System.out.println("Hi!");
                }
            }
        """.trimIndent(), submission = """
            public class Example {
                public static void sayHi() {
                    System.out.println("Bye :(");
                }
            }
        """.trimIndent(), className = "Example")
        Assert.assertTrue(result.classDesignAnalysisResult.all { it.result is Matched<*> })
        Assert.assertFalse(result.testSteps.filterIsInstance<ExecutedTestStep>().all { it.succeeded })
    }

}