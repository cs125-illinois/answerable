@file: JvmName("DefaultFrontend")
package edu.illinois.cs.cs125.answerable

import java.lang.IllegalStateException
import java.util.*

interface DefaultSerializable {
    fun toJson(): String
}

fun <E : DefaultSerializable> List<E>.toJson(): String =
        this.joinToString(prefix = "[", postfix = "]", transform = DefaultSerializable::toJson)

internal fun <T> TestOutput<T>.defaultToJson(): String {
    val specific = when (this.typeOfBehavior) {
        Behavior.RETURNED -> "  returned: \"${Regex.escape(output.toString())}\""
        Behavior.THREW -> "  threw: \"$threw\""
    }

    val stdOutputs = when (this.stdOut) {
        null -> ""
        else -> """
            |,
            |  stdOut: ${stdOut.jsonStringOrNull()},
            |  stdErr: ${stdErr.jsonStringOrNull()}"
        """.trimMargin()
    }

    return """
        |{
        |  resultType: "$typeOfBehavior",
        |  receiver: ${receiver.jsonStringOrNull()},
        |  args: ${args.joinToString(prefix = "[", postfix = "]", transform = ::fixArrayToString)},
        |$specific$stdOutputs
        |}
    """.trimMargin()
}

@Suppress("IMPLICIT_CAST_TO_ANY")
internal fun TestStep.defaultToJson(): String =
    """
        |{
        |  testNumber: $testNumber,
        |  refReceiver: ${refReceiver.jsonStringOrNull()},
        |  subReceiver: ${subReceiver.jsonStringOrNull()},
        |  succeeded: $succeeded,
        |  refOutput: ${refOutput.toJson()},
        |  subOutput: ${subOutput.toJson()},
        |  assertErr: ${assertErr.jsonStringOrNull()}
        |}
    """.trimMargin()

internal fun Any?.jsonStringOrNull(): String = when (this) {
    null -> "null"
    else -> "\"${Regex.escape(this.toString())}\""
}
internal fun fixArrayToString(thing: Any?): String = when (thing) {
    is Array<*> -> Arrays.deepToString(thing)
    else -> thing.toString()
}

internal fun TestRunOutput.defaultToJson(): String =
    """
        |{
        |  seed: $seed,
        |  testedClass: "${testedClass.canonicalName}",
        |  solutionName: "$solutionName",
        |  startTime: $startTime,
        |  endTime: $endTime,
        |  testSteps: ${testSteps.toJson()}
    """.trimMargin()

internal fun <T> AnalysisResult<T>.defaultToJson() = when (this) {
    is Matched -> """
        |{
        |  matched: true,
        |  found: ${found.showExpectedOrFound()}
        | }
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
        this as Pair<*, Array<*>>

        """
                    |{
                    |  superclass: "${this.first}"
                    |  interfaces: ${this.second.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }}
                    |}
                """.trimMargin()
    }
    else -> throw IllegalStateException("An analysis result contained an impossible type. Please report a bug.")
}