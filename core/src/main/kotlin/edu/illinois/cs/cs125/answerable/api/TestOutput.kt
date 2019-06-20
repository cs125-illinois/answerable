package edu.illinois.cs.cs125.answerable.api

import edu.illinois.cs.cs125.answerable.Behavior

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
) : DefaultSerializable {
    override fun toJson() = defaultToJson()

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
}