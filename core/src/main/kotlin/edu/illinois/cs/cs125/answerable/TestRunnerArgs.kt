@file: JvmName("Arguments")
package edu.illinois.cs.cs125.answerable

import edu.illinois.cs.cs125.answerable.api.OutputCapturer
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets

/**
 * A wrapper class for passing arguments to [TestRunner]s.
 */
data class TestRunnerArgs(
    /** The total number of tests to execute. */
    val numTests: Int = 1024,
    /** The maximum number of tests which can be discarded before Answerable gives up. */
    val maxDiscards: Int = 1024,
    /** The maximum number of edge-case only tests to execute. */
    val maxOnlyEdgeCaseTests: Int = numTests/16,
    /** The maximum number of simple-case only tests to execute. */
    val maxOnlySimpleCaseTests: Int = numTests/16,
    /** The total number of mixed edge- and simple-case tests to execute. */
    val numSimpleEdgeMixedTests: Int = numTests/16,
    /** The total number of all-generated tests to execute. */
    val numAllGeneratedTests: Int = numTests/2,
    /** The upper bound on the 'complexity' @[Generator] parameter for this test run. */
    val maxComplexity: Int = 100,
    /** The runner to use for capturing printed output. If null, [System.setOut] and [System.setErr] are used. */
    val outputCapturer: OutputCapturer? = null
)

/**
 * Answerable's default [TestRunnerArgs].
 *
 * Created with <tt>TestRunnerArgs(1024, 1024, 64, 64, 64, 512, 100, null)</tt>.
 */
val defaultArgs = TestRunnerArgs()

internal val defaultOutputCapturer = object : OutputCapturer {

    private var outText: String? = null
    private var errText: String? = null

    override fun runCapturingOutput(code: Runnable) {
        val oldOut = System.out
        val oldErr = System.err
        val newOut = ByteArrayOutputStream()
        val newErr = ByteArrayOutputStream()
        System.setOut(PrintStream(newOut))
        System.setErr(PrintStream(newErr))
        try {
            code.run()
        } finally {
            System.setOut(oldOut)
            System.setErr(oldErr)
            outText = newOut.toString(StandardCharsets.UTF_8)
            errText = newErr.toString(StandardCharsets.UTF_8)
            newOut.close()
            newErr.close()
        }
    }

    override fun getStandardOut() = outText
    override fun getStandardErr() = errText

}
