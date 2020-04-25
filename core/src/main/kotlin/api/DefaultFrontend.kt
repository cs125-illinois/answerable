@file: Suppress("TooManyFunctions", "MatchingDeclarationName")
@file: JvmName("DefaultFrontend")
package edu.illinois.cs.cs125.answerable.api

import com.google.gson.JsonPrimitive
import edu.illinois.cs.cs125.answerable.Behavior
import edu.illinois.cs.cs125.answerable.DiscardedTestStep
import edu.illinois.cs.cs125.answerable.ExecutedTestStep
import edu.illinois.cs.cs125.answerable.TestingResults
import edu.illinois.cs.cs125.answerable.classdesignanalysis.AnalysisOutput
import edu.illinois.cs.cs125.answerable.classdesignanalysis.AnalysisResult
import edu.illinois.cs.cs125.answerable.classdesignanalysis.Matched
import edu.illinois.cs.cs125.answerable.classdesignanalysis.Mismatched
import edu.illinois.cs.cs125.answerable.typeManagement.sourceName
import java.lang.IllegalStateException
import java.lang.reflect.Type
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

// TODO: Make all of this valid JSON

/**
 * Convert a [List] of [DefaultSerializable] objects to a [String] representing a JSON list.
 */
fun <E : DefaultSerializable> List<E>.toJson(): String =
        this.joinToString(prefix = "[", postfix = "]", transform = DefaultSerializable::toJson)

internal fun OssifiedTestOutput.defaultToJson(): String {
    val specific = when (this.typeOfBehavior) {
        Behavior.RETURNED -> "  returned: ${output?.value.jsonStringOrNull()}"
        Behavior.THREW -> "  threw: ${threw?.value.jsonStringOrNull()}"
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
        |  succeeded: $succeeded,
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
        |  classDesignAnalysisResult: ${classDesignAnalysisResult.toJson()},
        |  testSteps: ${testSteps.toJson()}
        |}
    """.trimMargin()

internal fun <T> AnalysisResult<T>.defaultToJson() = when (this) {
    is Matched -> """
        |{
        |  matched: true,
        |  found: ${found.showExpectedOrFound()}
        |}
    """.trimMargin()
    is Mismatched -> """
        |{
        |  matched: false,
        |  expected: ${expected.showExpectedOrFound()}
        |  found: ${found.showExpectedOrFound()}
        |}
    """.trimMargin()
}

internal fun AnalysisOutput.defaultToJson() = """
    |{
    |  tag: "$tag",
    |  matched: ${if (result is Matched) "true" else "false"},
    |  result: ${result.toJson()}
    |}
""".trimMargin()

private fun <T> T.showExpectedOrFound() = when (this) {
    is String -> "\"$this\""
    is List<*> -> this.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
    is Pair<*, *> -> {
        @Suppress("UNCHECKED_CAST")
        this as Pair<Type, Array<Type>>

        """
                    |{
                    |  superclass: "${this.first.sourceName}"
                    |  interfaces: ${this.second.joinToString(prefix = "[", postfix = "]") { "\"${it.sourceName}\"" }}
                    |}
                """.trimMargin()
    }
    else -> throw IllegalStateException("An analysis result contained an impossible type. Please report a bug.")
}
