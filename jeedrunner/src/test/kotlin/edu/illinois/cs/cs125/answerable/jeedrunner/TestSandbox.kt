package edu.illinois.cs.cs125.answerable.jeedrunner

import edu.illinois.cs.cs125.answerable.testing.ExecutedTestStep
import edu.illinois.cs.cs125.jeed.core.Sandbox
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class TestSandbox {

    private lateinit var originalOut: PrintStream

    @BeforeEach
    fun setup() {
        Sandbox.stop()
        originalOut = System.out
    }

    @AfterEach
    fun teardown() {
        Sandbox.stop()
        System.setOut(originalOut)
    }

    @Test
    fun testDangerousSubmission() {
        val result = testFromStrings(
            reference =
                """
            import edu.illinois.cs.cs125.answerable.annotations.Solution;
            public class Example {
                @Solution
                public static int getThing() {
                    return 5;
                }
            }
                """.trimIndent(),
            submission =
                """
            public class Example {
                public static int getThing() {
                    System.exit(5);
                    return 5;
                }
            }
                """.trimIndent(),
            className = "Example"
        )
        assertTrue(
            result.testSteps.filterIsInstance<ExecutedTestStep>().any {
                it.subDangerousLiveOutput.threw is SecurityException
            }
        )
    }

    @Test
    fun testSafeSubmission() {
        val result = testFromStrings(
            reference =
                """
            import edu.illinois.cs.cs125.answerable.annotations.Solution;
            public class Example {
                @Solution
                public static int getThing() {
                    return 5;
                }
            }
                """.trimIndent(),
            submission =
                """
            public class Example {
                public static int getThing() {
                    return 5;
                }
            }
                """.trimIndent(),
            className = "Example"
        )
        assertFalse(
            result.testSteps.filterIsInstance<ExecutedTestStep>()
                .any { it.subDangerousLiveOutput.threw is SecurityException }
        )
        assertTrue(result.testSteps.filterIsInstance<ExecutedTestStep>().all { it.succeeded })
    }

    @Test
    fun testPrintSuppression() {
        val output = ByteArrayOutputStream()
        System.setOut(PrintStream(output))
        val result = testFromStrings(
            reference =
                """
            import edu.illinois.cs.cs125.answerable.annotations.*;
            public class Example {
                @Solution
                public static int getThing() {
                    return 5;
                }
            }
                """.trimIndent(),
            submission =
                """
            public class Example {
                public static int getThing() {
                    System.out.println("noise");
                    return 5;
                }
            }
                """.trimIndent(),
            className = "Example"
        )
        assertEquals(0, output.size())
        assertTrue(result.testSteps.filterIsInstance<ExecutedTestStep>().all { it.succeeded })
    }

    @Test
    fun testTimeout() {
        val result = testFromStrings(
            reference =
                """
            import edu.illinois.cs.cs125.answerable.annotations.Solution;
            import edu.illinois.cs.cs125.answerable.annotations.Timeout;
            public class Example {
                @Solution
                @Timeout(timeout = 2000)
                public static int getThing() {
                    return 5;
                }
            }
                """.trimIndent(),
            submission =
                """
            public class Example {
                public static int getThing() {
                    wedge();
                    return 5;
                }
                private static void wedge() {
                    try {
                        while (true) {}
                    } catch (Throwable t) {
                        wedge();
                    } finally {
                        wedge();
                    }
                }
            }
                """.trimIndent(),
            className = "Example"
        )
        assertTrue(result.timedOut)
    }

    @Test
    fun testReferenceUnconstrained() {
        val result = testFromStrings(
            reference =
                """
            import edu.illinois.cs.cs125.answerable.annotations.Solution;
            public class Example {
                @Solution
                public static int getThing() {
                    System.getProperty("__nonsense_property__");
                    return 5;
                }
            }
                """.trimIndent(),
            submission =
                """
            public class Example {
                public static int getThing() {
                    return 5;
                }
            }
                """.trimIndent(),
            className = "Example"
        )
        assertFalse(
            result.testSteps.filterIsInstance<ExecutedTestStep>().any { it.refLiveOutput.threw is SecurityException }
        )
        assertTrue(result.testSteps.filterIsInstance<ExecutedTestStep>().all { it.succeeded })
    }
}
