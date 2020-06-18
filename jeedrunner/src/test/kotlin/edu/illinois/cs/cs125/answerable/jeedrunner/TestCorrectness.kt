package edu.illinois.cs.cs125.answerable.jeedrunner

import edu.illinois.cs.cs125.answerable.ExecutedTestStep
import edu.illinois.cs.cs125.answerable.InvertedClassloader
import edu.illinois.cs.cs125.answerable.TestGenerator
import edu.illinois.cs.cs125.answerable.TestRunnerArgs
import edu.illinois.cs.cs125.answerable.api.BytecodeProvider
import edu.illinois.cs.cs125.answerable.runTestsUnsecured
import edu.illinois.cs.cs125.jeed.core.CompilationArguments
import edu.illinois.cs.cs125.jeed.core.JeedClassLoader
import edu.illinois.cs.cs125.jeed.core.Source
import edu.illinois.cs.cs125.jeed.core.compile
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import kotlin.random.Random

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
                fail(it.toString())
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
                fail(it.toString())
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
    fun testPrinterrPackageCollision() {
        // Requires examples/Printerr.java to exist
        val result = testFromStrings(
            reference =
                """
            package examples;
            import edu.illinois.cs.cs125.answerable.annotations.Solution;
            public class Printerr {
                @Solution(prints=true)
                public static void welcome() {
                  System.out.println("Jeed");
                }
            }
                """.trimIndent(),
            submission =
                """
            package examples;
            public class Printerr {
                public static void welcome() {
                  System.out.println("Incorrect");
                }
            }
                """.trimIndent(),
            className = "examples.Printerr",
            testRunnerArgs = TestRunnerArgs(1)
        )
        result.assertSomethingFailed()
    }


    @Test
    fun testMirroringClassOnSystemClasspath() {
        // Depends on examples.Printerr existing on the class path
        val jeedClass = Source(
            mapOf(
                "Printerr.java" to """
                    package examples;
    
                    public class Printerr {
                        public static void welcome() {
                            System.out.println("Jeed");
                        }
                    }
                """.trimIndent()
            )
        ).compile(CompilationArguments(parentClassLoader = InvertedClassloader("examples.Printerr")))
            .classLoader.loadClass("examples.Printerr")

        val bytecodeProvider = object : BytecodeProvider {
            override fun getBytecode(clazz: Class<*>): ByteArray {
                return (jeedClass.classLoader as JeedClassLoader).bytecodeForClass(clazz.name)
            }
        }

        TestGenerator(examples.Printerr::class.java, bytecodeProvider = bytecodeProvider)
            .loadSubmission(jeedClass)
            .runTestsUnsecured(Random.nextLong())
            .assertSomethingFailed()
    }
}
