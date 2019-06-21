package edu.illinois.cs.cs125.answerable.api

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets

/**
 * An interface that allows Answerable to redirect print output.
 *
 * Applications must implement this interface if Answerable's default behavior of using [System.setOut]
 * and [System.setErr] is not acceptable.
 */
interface OutputRedirector {

    /**
     * Sets up stream redirection. Called before the task is run.
     */
    fun redirect()

    /**
     * Restores streams. Called after the task is run.
     */
    fun restore()

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

internal val defaultOutputRedirector = object : OutputRedirector {

    private lateinit var oldOut: PrintStream
    private lateinit var newOut: ByteArrayOutputStream
    private lateinit var oldErr: PrintStream
    private lateinit var newErr: ByteArrayOutputStream

    private var outText: String? = null
    private var errText: String? = null

    override fun redirect() {
        oldOut = System.out
        oldErr = System.err
        newOut = ByteArrayOutputStream()
        newErr = ByteArrayOutputStream()
        System.setOut(PrintStream(newOut))
        System.setErr(PrintStream(newErr))
    }

    override fun restore() {
        System.setOut(oldOut)
        System.setErr(oldErr)
        outText = newOut.toString(StandardCharsets.UTF_8)
        errText = newErr.toString(StandardCharsets.UTF_8)
        newOut.close()
        newErr.close()
    }

    override fun getStandardOut() = outText
    override fun getStandardErr() = errText

}
