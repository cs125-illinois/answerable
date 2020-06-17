package edu.illinois.cs.cs125.answerable.api

import edu.illinois.cs.cs125.answerable.Behavior
import edu.illinois.cs.cs125.answerable.annotations.Solution
import edu.illinois.cs.cs125.answerable.annotations.Verify
import edu.illinois.cs.cs125.answerable.classmanipulation.TypePool
import java.util.Arrays

/**
 * A wrapper class used to pass data to custom verification methods.
 */
data class TestOutput<T>(
    /**
     * An enum describing whether the method returned or threw an exception.
     *
     * If the test uses a standalone @[Verify] method, this will be [Behavior.VERIFY_ONLY].
     */
    val typeOfBehavior: Behavior,
    /** The object that the method was called on. Null if the method is static. */
    val receiver: T?,
    /** The arguments the method was called with */
    val args: Array<Any?>,
    /** The return value of the method. If [threw] is not null, [output] is always null. */
    val output: Any?,
    /** The throwable (if any) thrown by the method. Null if nothing was thrown. */
    val threw: Throwable?,
    /** The log of stdOut during the method invocation. Only non-null if [Solution.prints] is true. */
    val stdOut: String?,
    /** The log of stdErr during the method invocation. Only non-null if [Solution.prints] is true. */
    val stdErr: String?
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TestOutput<*>

        if (receiver != other.receiver) return false
        if (!args.contentEquals(other.args)) return false
        if (output != other.output) return false
        if (threw != other.threw) return false
        if (stdOut != other.stdOut) return false
        if (stdErr != other.stdErr) return false

        return true
    }

    override fun hashCode(): Int {
        var result = receiver?.hashCode() ?: 0
        result = 31 * result + args.contentHashCode()
        result = 31 * result + (output?.hashCode() ?: 0)
        result = 31 * result + (threw?.hashCode() ?: 0)
        result = 31 * result + (stdOut?.hashCode() ?: 0)
        result = 31 * result + (stdErr?.hashCode() ?: 0)
        return result
    }

    internal fun ossify(pool: TypePool): OssifiedTestOutput {
        return OssifiedTestOutput(
            typeOfBehavior = typeOfBehavior,
            receiver = receiver.ossify(pool),
            args = args.map { it.ossify(pool) }.toTypedArray(),
            output = output.ossify(pool),
            threw = threw.ossify(pool),
            stdOut = stdOut,
            stdErr = stdErr
        )
    }
}

/**
 * A version of [TestOutput] containing no live objects from the testing run.
 */
data class OssifiedTestOutput(
    val typeOfBehavior: Behavior,
    val receiver: OssifiedValue?,
    val args: Array<OssifiedValue?>,
    val output: OssifiedValue?,
    val threw: OssifiedValue?,
    val stdOut: String?,
    val stdErr: String?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as OssifiedTestOutput

        if (typeOfBehavior != other.typeOfBehavior) return false
        if (receiver != other.receiver) return false
        if (!args.contentEquals(other.args)) return false
        if (output != other.output) return false
        if (threw != other.threw) return false
        if (stdOut != other.stdOut) return false
        if (stdErr != other.stdErr) return false

        return true
    }

    override fun hashCode(): Int {
        var result = typeOfBehavior.hashCode()
        result = 31 * result + (receiver?.hashCode() ?: 0)
        result = 31 * result + args.contentHashCode()
        result = 31 * result + (output?.hashCode() ?: 0)
        result = 31 * result + (threw?.hashCode() ?: 0)
        result = 31 * result + (stdOut?.hashCode() ?: 0)
        result = 31 * result + (stdErr?.hashCode() ?: 0)
        return result
    }
}

data class OssifiedValue internal constructor(
    val type: String,
    val value: String,
    val identity: Int
) {
    override fun toString(): String = value
}

internal fun Any?.ossify(pool: TypePool): OssifiedValue? {
    fun safeStringify(block: () -> String): String {
        @Suppress("TooGenericExceptionCaught")
        return try {
            block()
        } catch (t: Throwable) {
            try {
                "<failed to stringify ${this?.javaClass?.name}: ${t.message}>"
            } catch (_: Throwable) {
                // Getting the error's message could conceivably crash
                "<stringification of ${this?.javaClass?.name} double-faulted>"
            }
        }
    }
    return when {
        this == null -> {
            null
        }
        this is Array<*> -> {
            var componentType = this.javaClass.componentType
            var nestingLevel = 1
            while (componentType.isArray) {
                componentType = componentType.componentType
                nestingLevel++
            }
            val originalName = pool.getOriginalClass(componentType).typeName
            OssifiedValue(
                originalName + "[]".repeat(nestingLevel),
                safeStringify { this.contentDeepToString() }.replace(this.javaClass.name, originalName),
                System.identityHashCode(this)
            )
        }
        this.javaClass.isArray -> {
            // Primitive array type
            val componentType = this.javaClass.componentType
            val stringifier = Arrays::class.java.getMethod("toString", this.javaClass)
            OssifiedValue(
                "$componentType[]",
                safeStringify { stringifier(null, this) as String },
                System.identityHashCode(this)
            )
        }
        else -> {
            val originalName = pool.getOriginalClass(this.javaClass).typeName
            OssifiedValue(
                originalName,
                safeStringify { this.toString() }.replace(this.javaClass.name, originalName),
                System.identityHashCode(this)
            )
        }
    }
}
