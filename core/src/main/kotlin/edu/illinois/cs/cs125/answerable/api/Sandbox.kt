package edu.illinois.cs.cs125.answerable.api

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Represents a secured execution environment for testing untrusted submissions.
 */
interface Sandbox {

    /**
     * Runs a testing task on an untrusted submission. The untrusted classloader may be transformed before
     * being passed to [SandboxedRunner.invoke]. The fully-qualified names of classes in [untrustedLoader]
     * must be preserved.
     *
     * @param untrustedLoader [ClassLoader] containing untrusted classes
     * @param timeout timeout in milliseconds or zero for untimed
     * @param callback testing task to run in the sandbox
     * @return whether the task completed in time
     */
    fun runInSandbox(untrustedLoader: EnumerableBytecodeProvidingClassLoader, timeout: Long, callback: SandboxedRunner): Boolean

}

/**
 * Represents a [ClassLoader] that can provide the bytecode of all classes that are loaded into it.
 *
 * Do not implement this interface. Answerable implements it so that a custom [Sandbox] can access
 * and rewrite the bytecode of untrusted classes. All instances of this interface are also instances
 * of [ClassLoader].
 */
interface EnumerableBytecodeProvidingClassLoader : BytecodeProvider {

    /**
     * Retrieves a map from fully qualified class names to bytecode.
     *
     * The map contains all classes that have been and will ever be loaded by this classloader.
     */
    fun getAllBytecode(): Map<String, ByteArray>

}

/**
 * A callback to test an untrusted submission in a sandbox.
 *
 * Do not implement this interface. Answerable implements it so that a custom [Sandbox] can call
 * back into the test runner after untrusted classes have been transformed.
 */
interface SandboxedRunner {

    /**
     * Runs the test suite. [sandboxedLoader] can load transformed classes of the same names as the classes
     * in the loader passed to [Sandbox.runInSandbox]. The original parent of the untrusted classloader must be
     * somewhere in the classloader hierarchy of the sandboxed classloader. [sandboxedBytecodeProvider] must be
     * able to provide the bytecode of all untrusted classes in the sandboxed classloader.
     */
    operator fun invoke(sandboxedLoader: ClassLoader, sandboxedBytecodeProvider: BytecodeProvider)

}

/**
 * The default (insecure!) sandbox, which can handle timeouts for nice submissions but provides no other security.
 */
internal val defaultSandbox = object : Sandbox {
    override fun runInSandbox(untrustedLoader: EnumerableBytecodeProvidingClassLoader, timeout: Long, callback: SandboxedRunner): Boolean {
        fun timedTestingPortion() {
            callback(untrustedLoader as ClassLoader, untrustedLoader)
        }
        return if (timeout == 0L) {
            timedTestingPortion()
            true
        } else {
            try {
                Executors.newSingleThreadExecutor().submit(::timedTestingPortion)[timeout, TimeUnit.MILLISECONDS]
                true
            } catch (e: TimeoutException) {
                false
            }
        }
    }
}
