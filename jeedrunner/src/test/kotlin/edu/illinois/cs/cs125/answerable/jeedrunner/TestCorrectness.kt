package edu.illinois.cs.cs125.answerable.jeedrunner

import edu.illinois.cs.cs125.answerable.ExecutedTestStep
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class TestCorrectness {

    @Test
    fun testPrintOutputCorrect() {
        val result = testFromStrings(
            reference =
                """
            import edu.illinois.cs.cs125.answerable.annotations.Solution;
            public class Example {
                @Solution(prints = true)
                public static void sayHi() {
                    System.out.println("Hi!");
                }
            }
                """.trimIndent(),
            submission =
                """
            public class Example {
                public static void sayHi() {
                    System.out.print("Hi");
                    System.out.println("!");
                }
            }
                """.trimIndent(),
            className = "Example"
        )
        result.testSteps.filterIsInstance<ExecutedTestStep>().forEach {
            if (!it.succeeded) {
                fail(it.toJson())
            }
        }
    }

    @Test
    fun testPrintOutputWrong() {
        val result = testFromStrings(
            reference =
                """
            import edu.illinois.cs.cs125.answerable.annotations.Solution;
            public class Example {
                @Solution(prints = true)
                public static void sayHi() {
                    System.out.println("Hi!");
                }
            }
                """.trimIndent(),
            submission =
                """
            public class Example {
                public static void sayHi() {
                    System.out.println("Bye :(");
                }
            }
                """.trimIndent(),
            className = "Example"
        )
        assertTrue(result.classDesignAnalysisResult.allMatch)
        assertFalse(result.testSteps.filterIsInstance<ExecutedTestStep>().all { it.succeeded })
    }

    @Test
    fun testAdderCorrect() {
        val result = testFromStrings(
            reference =
                """
            import edu.illinois.cs.cs125.answerable.annotations.Solution;
            public class Adder {
                @Solution
                public static int add(int a, int b) {
                    return a + b;
                }
            }
                """.trimIndent(),
            submission =
                """
            public class Adder {
                public static int add(int one, int two) {
                    return two + one;
                }
            }
                """.trimIndent(),
            className = "Adder"
        )
        result.testSteps.filterIsInstance<ExecutedTestStep>().forEach {
            if (!it.succeeded) {
                fail(it.toJson())
            }
        }
    }

    @Test
    fun testAdderWrong() {
        val result = testFromStrings(
            reference =
                """
            import edu.illinois.cs.cs125.answerable.annotations.Solution;
            public class Adder {
                @Solution
                public static int add(int a, int b) {
                    return a + b;
                }
            }
                """.trimIndent(),
            submission =
                """
            public class Adder {
                public static int add(int a, int b) {
                    return a + Math.abs(b);
                }
            }
                """.trimIndent(),
            className = "Adder"
        )
        assertTrue(result.classDesignAnalysisResult.allMatch)
        assertFalse(result.testSteps.filterIsInstance<ExecutedTestStep>().all { it.succeeded })
    }

    @Test
    fun testAdderrPackageCollision() {
        // Requires examples/Adderr.java to exist
        val result = testFromStrings(
            reference =
                """
            package examples;
            import edu.illinois.cs.cs125.answerable.annotations.Solution;
            public class Adderr {
                @Solution
                public static int add(int a, int b) {
                    return a + b;
                }
            }
                """.trimIndent(),
            submission =
                """
            package examples;
            public class Adderr {
                public static int add(int a, int b) {
                    return a + Math.abs(b);
                }
            }
                """.trimIndent(),
            className = "examples.Adderr"
        )
        assertTrue(result.classDesignAnalysisResult.allMatch)
        assertFalse(result.testSteps.filterIsInstance<ExecutedTestStep>().all { it.succeeded })
    }
}
