package edu.illinois.cs.cs125.answerable.api

/**
 * Represents a secured execution environment for testing untrusted submissions.
 */
interface Sandbox {

    /**
     * Runs a testing task on an untrusted submission. The untrusted classloader may be transformed before
     * being passed to [SandboxedRunner.invoke]. The fully-qualified names of classes in [untrustedLoader]
     * must be preserved.
     */
    fun runInSandbox(untrustedLoader: EnumerableBytecodeProvidingClassLoader, callback: SandboxedRunner)

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

internal val insecureSandbox = object : Sandbox {
    override fun runInSandbox(untrustedLoader: EnumerableBytecodeProvidingClassLoader, callback: SandboxedRunner) {
        callback(untrustedLoader as ClassLoader, untrustedLoader)
    }
}
