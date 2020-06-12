@file:JvmName("AnswerableJeedAdapter")

package edu.illinois.cs.cs125.answerable.jeedrunner

import edu.illinois.cs.cs125.answerable.api.BytecodeClassProvider
import edu.illinois.cs.cs125.answerable.api.BytecodeProvider
import edu.illinois.cs.cs125.answerable.api.EnumerableBytecodeLoader
import edu.illinois.cs.cs125.answerable.api.OutputCapturer
import edu.illinois.cs.cs125.jeed.core.JeedClassLoader
import edu.illinois.cs.cs125.jeed.core.Sandbox
import edu.illinois.cs.cs125.jeed.core.sandbox
import kotlinx.coroutines.runBlocking
import kotlin.math.min

/**
 * The Answerable [OutputCapturer] that redirects output using Jeed's [Sandbox.redirectOutput].
 */
val jeedOutputCapturer = object : OutputCapturer {

    private var stdOut: String? = null
    private var stdErr: String? = null

    override fun runCapturingOutput(code: Runnable) {
        Sandbox.redirectOutput { code.run() }.also {
            stdOut = it.stdout
            stdErr = it.stderr
        }
    }

    override fun getStandardOut(): String? = stdOut
    override fun getStandardErr(): String? = stdErr
}

private val ALWAYS_BANNED_CLASSES = Sandbox.ClassLoaderConfiguration.DEFAULT_BLACKLISTED_CLASSES +
    setOf("edu.illinois.cs.cs125.answerable.")

/**
 * Creates an Answerable sandbox that secures the environment using Jeed's [Sandbox].
 * The [executeConfig]'s timeout, maxOutputLines, and classLoaderConfiguration will be replaced, but other settings
 * can be configured.
 *
 * @param loaderConfig class loading restrictions for the submission
 * @param executeConfig other sandbox restrictions for the submission
 * @param maxTimeout the longest the whole Answerable testing worker is allowed to run
 * @return an Answerable sandbox
 */
fun jeedSandbox(
    loaderConfig: Sandbox.ClassLoaderConfiguration = Sandbox.ClassLoaderConfiguration(),
    executeConfig: Sandbox.ExecutionArguments = Sandbox.ExecutionArguments(),
    maxTimeout: Long = 10000L
): edu.illinois.cs.cs125.answerable.api.Sandbox {
    return object : edu.illinois.cs.cs125.answerable.api.Sandbox {
        private lateinit var sandboxedLoader: Sandbox.SandboxedClassLoader
        override fun transformLoader(loader: EnumerableBytecodeLoader): BytecodeClassProvider {
            val sandboxableLoader = object : Sandbox.SandboxableClassLoader {
                override val bytecodeForClasses: Map<String, ByteArray> = loader.getAllBytecode()
                override val classLoader: ClassLoader = loader.getLoader()
            }
            sandboxedLoader = sandboxableLoader.sandbox(loaderConfig)
            return object : BytecodeClassProvider {
                override fun getLoader() = sandboxedLoader
                override fun getBytecode(clazz: Class<*>): ByteArray {
                    return sandboxedLoader.knownClasses[clazz.name]
                        ?: throw ClassNotFoundException("Jeed did not provide $clazz")
                }
            }
        }
        override fun run(timeout: Long?, callback: Runnable): Boolean {
            val filteredLoaderConfig = if (loaderConfig.whitelistedClasses.isEmpty()) {
                Sandbox.ClassLoaderConfiguration(
                    blacklistedClasses = loaderConfig.blacklistedClasses + ALWAYS_BANNED_CLASSES,
                    unsafeExceptions = loaderConfig.unsafeExceptions,
                    isolatedClasses = loaderConfig.isolatedClasses
                )
            } else {
                loaderConfig
            }
            val timeoutConfig = Sandbox.ExecutionArguments(
                timeout = min(timeout ?: Long.MAX_VALUE, maxTimeout),
                permissions = executeConfig.permissions,
                maxExtraThreads = executeConfig.maxExtraThreads,
                waitForShutdown = executeConfig.waitForShutdown,
                classLoaderConfiguration = filteredLoaderConfig,
                maxOutputLines = Int.MAX_VALUE
            )
            val job: (Pair<ClassLoader, (() -> Unit) -> Sandbox.RedirectedOutput>) -> Any? = { callback.run() }
            val result = runBlocking {
                Sandbox.execute(sandboxedLoader, timeoutConfig, job)
            }
            return !result.timeout
        }
    }
}

/**
 * Creates an Answerable [BytecodeProvider] that provides the bytecode of classes loaded by a [JeedClassLoader].
 *
 * @param loader the Jeed class loader
 * @return a bytecode provider responsible for all classes defined by Jeed in [loader]
 */
fun answerableBytecodeProvider(loader: JeedClassLoader): BytecodeProvider {
    return object : BytecodeProvider {
        override fun getBytecode(clazz: Class<*>): ByteArray {
            return loader.bytecodeForClass(clazz.name)
        }
    }
}
