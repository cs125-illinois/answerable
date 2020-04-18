@file: Suppress("UNCHECKED_CAST")
package edu.illinois.cs.cs125.answerable

import java.lang.IllegalStateException
import java.lang.reflect.Type

internal fun AnalysisOutput.toErrorMsg() = when (this.result) {
    is Matched -> "Everything looks good!"
    is Mismatched -> when (this.tag) {
        AnalysisTag.NAME -> ::mkNameError
        AnalysisTag.STATUS -> ::mkStatusError
        AnalysisTag.MODIFIERS -> ::mkModifierError
        AnalysisTag.TYPE_PARAMS -> ::mkTypeParamError
        AnalysisTag.SUPERCLASSES -> ::mkSuperClassError
        AnalysisTag.FIELDS -> ::mkFieldError
        AnalysisTag.METHODS -> ::mkMethodError
    }(this.result)
}

/** Expects Mismatched<String> */
private fun mkNameError(result: Mismatched<*>) =
        """
            |Class name mismatch!
            |Expected : ${result.expected}
            |Found    : ${result.found}
        """.trimMargin()

/** Expected Mismatched<String> */
private fun mkStatusError(result: Mismatched<*>): String {
    result as Mismatched<String>

    fun article(s: String) = if (s[0] in "aeiou") "an $s" else "a $s"

    return """
        |Class status mismatch!
        |Expected : ${article(result.expected)}
        |Found    : ${article(result.found)}
    """.trimMargin()
}

private fun mkModifierError(result: Mismatched<*>): String {
    result as Mismatched<String>

    return """
        |Class modifiers mismatch!
        |Expected : ${result.expected}
        |Found    : ${result.found}
    """.trimMargin()
}

private fun mkTypeParamError(result: Mismatched<*>): String {
    val expected = result.expected as List<*>
    val singleE = expected.size == 1
    val actual = result.found as List<*>
    val singleA = actual.size == 1

    val buff = "   "
    val spaces =
        if (singleE == singleA) ("$buff ")
        else if (singleE && !singleA) buff
        else "$buff  "
    return "Expected type parameter${if (singleE) "" else "s"} : ${expected.joinToString(prefix = "<", postfix = ">")}\n" +
            "Found type parameter${if (singleA) "" else "s"}$spaces: ${actual.joinToString(prefix = "<", postfix = ">")}"
}

private fun mkSuperClassError(result: Mismatched<*>): String {
    result as Mismatched<Pair<Type?, Array<out Type>>>

    return mkSuperclassMismatchErrorMsg(result.expected.first, result.expected.second, result.found.first, result.found.second)
}

private fun mkSuperclassMismatchErrorMsg(
    referenceSC: Type?, referenceI: Array<out Type>, attemptSC: Type?, attemptI: Array<out Type>
): String {
    val scMatch = referenceSC == attemptSC
    val iMatch = referenceI contentEquals attemptI
    val refExtendsSomething = referenceSC != Object::class.java && referenceSC != null
    val attemptExtendsSomething = attemptSC != Object::class.java && attemptSC != null
    val refImplementsInterfaces = referenceI.isNotEmpty()
    val attemptImplementsInterfaces = attemptI.isNotEmpty()

    val msg: StringBuilder = StringBuilder("Expected class ")
    var also = false
    if (!scMatch) {
        if (refExtendsSomething) {
            msg.append("to extend `${referenceSC!!.typeName}', ") // refExtendsSomething => referenceSC != null
        } else {
            msg.append("to not extend any classes, ")
        }

        if (attemptExtendsSomething) {
            msg.append("but class extended `${attemptSC!!.typeName}'") // attemptExtendsSomething => attemptSC != null
        } else {
            msg.append("but class did not extend any classes")
        }
        also = true
    }
    if (!iMatch) {
        if (also) msg.append(";\nAlso expected class ")

        if (refImplementsInterfaces) {
            msg.append("to implement ${referenceI.joinToStringLast(last = ", and ") { "`${it.typeName}'" }}, ")
        } else {
            msg.append("to not implement any interfaces, ")
        }

        if (attemptImplementsInterfaces) {
            msg.append("but class implemented ${attemptI.joinToStringLast(last = ", and ") { "`${it.typeName}'" }}")
        } else {
            msg.append("but class did not implement any interfaces")
        }
    }
    msg.append(".")

    // If this function was called, then it must be the case that either !scMatch or !iMatch.
    // Therefore the bad message "Expected class."  can never occur.
    return msg.toString()
}

private fun <T> Array<out T>.joinToStringLast(
    separator: CharSequence = ", ",
    prefix: CharSequence = "",
    last: CharSequence = "",
    postfix: CharSequence = "",
    transform: ((T) -> CharSequence)? = null
): String {
    val safeTransform = transform ?: { it }

    if (this.size <= 1) {
        return this.joinToString(separator, prefix, postfix, transform = transform)
    }

    return (this.sliceArray(0 until this.size - 1)
        .joinToString(separator, prefix, postfix = "", transform = transform)
            + last + safeTransform(this.last()) + postfix)
}

private fun mkFieldError(result: Mismatched<*>): String {
    result as Mismatched<List<*>>

    return mkPublicApiMismatchMsg(result.expected, result.found, "field")
}

private fun mkMethodError(result: Mismatched<*>): String {
    result as Mismatched<List<*>>

    return mkPublicApiMismatchMsg(result.expected, result.found, "method")
}

private fun <T> mkPublicApiMismatchMsg(allExpected: List<T>, allActual: List<T>, apiTypeName: String): String {

    val exp = allExpected.filter { it !in allActual }
    val act = allActual.filter { it !in allExpected }

    return when (Pair(exp.isEmpty(), act.isEmpty())) {
        Pair(true, false) -> {
            val single = act.size == 1
            """
                    |Found ${if (single) "an " else ""}unexpected public $apiTypeName${if (single) "" else "s"}:
                    |${act.joinToString(separator = "\n  ", prefix = "  ")}
                """.trimMargin()
        }
        Pair(false, true) -> {
            val single = exp.size == 1
            """
                    |Expected ${if (single) "another" else "more"} public $apiTypeName${if (single) "" else "s"}:
                    |${exp.joinToString(separator = "\n  ", prefix = "  ")}
                """.trimMargin()
        }
        Pair(false, false) -> {
            // In this case the exact cause of the mismatch is provably undecidable
            // in the future we can try and provide better guesses here, but for now we'll dump everything
            """
                    |Expected your class to have public $apiTypeName${if (exp.size == 1) "" else "s"}:
                    |${exp.joinToString(separator = "\n  ", prefix = "  ")}
                    |but found public $apiTypeName${if (act.size == 1) "" else "s"}:
                    |${act.joinToString(separator = "\n  ", prefix = "  ")}
                """.trimMargin()
        }
        else -> throw IllegalStateException("Tried to generate API mismatch error, but no mismatch was found.\nPlease report a bug.")
    }
}