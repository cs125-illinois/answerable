package edu.illinois.cs.cs125.answerable

import org.junit.*
import java.io.ByteArrayOutputStream
import java.io.PrintStream

class TestSandbox {

    /*private lateinit var originalOut: PrintStream

    @Before
    fun setup() {
        originalOut = System.out
    }

    @After
    fun teardown() {
        System.setOut(originalOut)
    }*/

    @Test
    fun testDangerousSubmission() {
        val result = testFromStrings(reference = """
            import edu.illinois.cs.cs125.answerable.api.*;
            public class Example {
                @Solution
                public static int getThing() {
                    return 5;
                }
            }
        """.trimIndent(), submission = """
            public class Example {
                public static int getThing() {
                    System.exit(5);
                    return 5;
                }
            }
        """.trimIndent(), className = "Example")
        Assert.assertTrue(result.testSteps.filterIsInstance<ExecutedTestStep>().any { it.subDangerousLiveOutput.threw is SecurityException })
    }

    @Test
    fun testSafeSubmission() {
        val result = testFromStrings(reference = """
            import edu.illinois.cs.cs125.answerable.api.*;
            public class Example {
                @Solution
                public static int getThing() {
                    return 5;
                }
            }
        """.trimIndent(), submission = """
            public class Example {
                public static int getThing() {
                    return 5;
                }
            }
        """.trimIndent(), className = "Example")
        Assert.assertFalse(result.testSteps.filterIsInstance<ExecutedTestStep>().any { it.subDangerousLiveOutput.threw is SecurityException })
        Assert.assertTrue(result.testSteps.filterIsInstance<ExecutedTestStep>().all { it.succeeded })
    }

    @Test
    @Ignore("Jeed does not play nicely with System.setOut")
    fun testPrintSuppression() {
        val output = ByteArrayOutputStream()
        System.setOut(PrintStream(output))
        val result = testFromStrings(reference = """
            import edu.illinois.cs.cs125.answerable.api.*;
            public class Example {
                @Solution
                public static int getThing() {
                    return 5;
                }
            }
        """.trimIndent(), submission = """
            public class Example {
                public static int getThing() {
                    System.out.println("noise");
                    return 5;
                }
            }
        """.trimIndent(), className = "Example")
        Assert.assertEquals(0, output.size())
        Assert.assertTrue(result.testSteps.filterIsInstance<ExecutedTestStep>().all { it.succeeded })
    }

    @Test
    fun testTimeout() {
        val result = testFromStrings(reference = """
            import edu.illinois.cs.cs125.answerable.api.*;
            public class Example {
                @Solution
                public static int getThing() {
                    return 5;
                }
            }
        """.trimIndent(), submission = """
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
        """.trimIndent(), className = "Example")
        Assert.assertTrue(result.timedOut)
    }

    @Test
    fun testReferenceUnconstrained() {
        val result = testFromStrings(reference = """
            import edu.illinois.cs.cs125.answerable.api.*;
            public class Example {
                @Solution
                public static int getThing() {
                    System.getProperty("__nonsense_property__");
                    return 5;
                }
            }
        """.trimIndent(), submission = """
            public class Example {
                public static int getThing() {
                    return 5;
                }
            }
        """.trimIndent(), className = "Example")
        Assert.assertFalse(result.testSteps.filterIsInstance<ExecutedTestStep>().any { it.refLiveOutput.threw is SecurityException })
        Assert.assertTrue(result.testSteps.filterIsInstance<ExecutedTestStep>().all { it.succeeded })
    }

}
