package edu.illinois.cs.cs125.answerable.api

/**
 * An interface that allows Answerable to run a task and capture its printed output.
 *
 * Applications must implement this interface if Answerable's default behavior of using [System.setOut]
 * and [System.setErr] is not acceptable.
 */
interface OutputCapturer {

    /**
     * Runs the task whose [System.out] and [System.err] output should be captured.
     * @param code the task to run
     */
    fun runCapturingOutput(code: Runnable)

    /**
     * Gets text printed to the standard output stream.
     * @return the text the task sent to System.out
     */
    fun getStandardOut(): String?

    /**
     * Gets text printed to the standard error stream.
     * @return the text the task sent to System.err
     */
    fun getStandardErr(): String?

}
