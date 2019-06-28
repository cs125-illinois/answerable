package edu.illinois.cs.cs125.answerable

import edu.illinois.cs.cs125.answerable.api.OutputCapturer
import edu.illinois.cs.cs125.answerable.api.Sandbox
import edu.illinois.cs.cs125.answerable.api.defaultOutputCapturer
import edu.illinois.cs.cs125.answerable.api.defaultSandbox

/**
 * A wrapper class for specifying the environment in which tests are run.
 */
data class TestEnvironment(
        /** The runner to use for capturing printed output. */
        val outputCapturer: OutputCapturer,
        /** The sandbox for executing tests. */
        val sandbox: Sandbox
) {
        companion object {
                @JvmStatic
                fun getUnsecuredEnvironment(): TestEnvironment = defaultEnvironment
        }
}

/**
 * A default, simple environment.
 * Output is captured by redirecting [System.out] and [System.err].
 * Timeouts are enforced (on non-pathological submissions), but no security restrictions are applied.
 */
val defaultEnvironment = TestEnvironment(defaultOutputCapturer, defaultSandbox)
