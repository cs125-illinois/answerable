@file: Suppress("TooManyFunctions", "MatchingDeclarationName")
@file: JvmName("DefaultFrontend")
package edu.illinois.cs.cs125.answerable.api

import com.google.gson.JsonPrimitive
import edu.illinois.cs.cs125.answerable.Behavior
import edu.illinois.cs.cs125.answerable.DiscardedTestStep
import edu.illinois.cs.cs125.answerable.ExecutedTestStep
import edu.illinois.cs.cs125.answerable.TestingResults
import java.util.Arrays

/**
 * An interface for Answerable-internal objects which have default JSON serialization methods.
 */
interface DefaultSerializable {
    /**
     * Convert this object to a non-pretty-printed JSON string.
     *
     * No guarantees are made about the formatting other than that the string is valid JSON.
     */
    fun toJson(): String
}

// TODO: Remove all this

/**
 * Convert a [List] of [DefaultSerializable] objects to a [String] representing a JSON list.
 */
fun <E : DefaultSerializable> List<E>.toJson(): String =
    this.joinToString(prefix = "[", postfix = "]", transform = DefaultSerializable::toJson)

internal fun OssifiedTestOutput.defaultToJson(): String {
    val specific = when (this.typeOfBehavior) {
        Behavior.RETURNED -> "  returned: ${output?.value.jsonStringOrNull()}"
        Behavior.THREW, Behavior.GENERATION_FAILED -> "  threw: ${threw?.value.jsonStringOrNull()}"
        Behavior.VERIFY_ONLY -> ""
    }

    val stdOutputs = when (this.stdOut) {
        null -> ""
        else -> """
            |  stdOut: ${stdOut.jsonStringOrNull()},
            |  stdErr: ${stdErr.jsonStringOrNull()}
        """.trimMargin()
    }

    return """
        |{
        |  resultType: "$typeOfBehavior",
        |  receiver: ${receiver?.value.jsonStringOrNull()},
        |  args: ${args.map { it?.value }
        .joinToString(prefix = "[", postfix = "]", transform = ::fixArrayToString)}${if (specific == "") "" else ","}
        |$specific${if (stdOutputs == "") "" else ",\n"}$stdOutputs
        |}
    """.trimMargin()
}

internal fun ExecutedTestStep.defaultToJson(): String =
    """
        |{
        |  testNumber: $testNumber,
        |  testType: $testType,
        |  discarded: false,
        |  refReceiver: ${refReceiver?.value.jsonStringOrNull()},
        |  subReceiver: ${subReceiver?.value.jsonStringOrNull()},
        |  succeeded: $testSucceeded,
        |  refOutput: ${refOutput.toJson()},
        |  subOutput: ${subOutput.toJson()},
        |  assertErr: ${assertErr.jsonStringOrNull()}
        |}
    """.trimMargin()

internal fun DiscardedTestStep.defaultToJson(): String =
    """
        |{
        |  testNumber: $testNumber,
        |  testType: $testType
        |  discarded: true,
        |  receiver: $receiver,
        |  args: ${args.joinToString(prefix = "[", postfix = "]", transform = ::fixArrayToString)}
        |}
    """.trimMargin()

internal fun Any?.jsonStringOrNull(): String = when (this) {
    null -> "null"
    else -> this.toString().escape()
}
internal fun String.escape(): String = JsonPrimitive(this).toString()

internal fun fixArrayToString(thing: Any?): String = when (thing) {
    is Array<*> -> Arrays.deepToString(thing)
    else -> thing.toString()
}

internal fun TestingResults.defaultToJson(): String =
    """
        |{
        |  seed: $seed,
        |  referenceClass: "${referenceClass.canonicalName}",
        |  testedClass: "${testedClass.canonicalName}",
        |  solutionName: "$solutionName",
        |  startTime: $startTime,
        |  endTime: $endTime,
        |  timedOut: $timedOut,
        |  numDiscardedTests: $numDiscardedTests,
        |  numTests: $numTests,
        |  numEdgeCaseTests: $numEdgeCaseTests,
        |  numSimpleCaseTests: $numSimpleCaseTests,
        |  numSimpleAndEdgeCaseTests: $numSimpleAndEdgeCaseTests,
        |  numMixedTests: $numMixedTests,
        |  numAllGeneratedTests: $numAllGeneratedTests,
        |  classDesignAnalysisResult: ${classDesignAnalysisResult.messages.joinToString(separator = ",")},
        |  testSteps: ${testSteps.toJson()}
        |}
    """.trimMargin()
