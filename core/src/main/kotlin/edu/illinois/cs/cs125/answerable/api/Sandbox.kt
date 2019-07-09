package edu.illinois.cs.cs125.answerable.api

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Represents a secured execution environment for testing untrusted submissions.
 */
interface Sandbox {

    /**
     * Transforms untrusted classes into a new [ClassLoader]. The fully-qualified names of all classes
     * in the [loader] must be preserved.
     */
    fun transformLoader(loader: EnumerableBytecodeLoader): BytecodeClassProvider

    /**
     * Runs a testing task on an untrusted submission.
     *
     * @param timeout timeout in milliseconds or null for untimed
     * @param callback testing task to run in the sandbox
     * @return whether the task completed in time
     */
    fun run(timeout: Long?, callback: Runnable): Boolean

}

/**
 * Wraps a [ClassLoader] that can provide the bytecode of all the classes it loaded.
 */
interface BytecodeClassProvider : BytecodeProvider {

    /**
     * Gets the classloader responsible for the classes in this bytecode provider.
     */
    fun getLoader(): ClassLoader

}

/**
 * Allows access to a classloader and the bytecode it loaded.
 *
 * Do not implement this interface. Answerable implements it so that a custom [Sandbox] can access
 * and rewrite the bytecode of untrusted classes.
 */
interface EnumerableBytecodeLoader : BytecodeClassProvider {

    /**
     * Retrieves a map from fully qualified class names to bytecode.
     *
     * The map contains all classes that have been and will ever be loaded by the classloader.
     */
    fun getAllBytecode(): Map<String, ByteArray>

}

/**
 * The default (insecure!) sandbox, which can handle timeouts for nice submissions but provides no other security.
 */
internal val defaultSandbox = object : Sandbox {

    override fun transformLoader(loader: EnumerableBytecodeLoader): BytecodeClassProvider {
        return loader
    }

    override fun run(timeout: Long?, callback: Runnable): Boolean {
        fun timedTestingPortion() {
            callback.run()
        }
        return if (timeout == null) {
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

/**
 * A "sandbox" that just runs the task in the current thread with no security restrictions.
 * Used for dry runs, where timeout is handled by TestGenerator.
 */
internal val sameThreadSandbox = object : Sandbox {

    override fun transformLoader(loader: EnumerableBytecodeLoader): BytecodeClassProvider {
        return loader
    }

    override fun run(timeout: Long?, callback: Runnable): Boolean {
        callback.run()
        return true
    }

}
