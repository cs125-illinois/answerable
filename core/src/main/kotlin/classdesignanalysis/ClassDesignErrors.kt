@file:Suppress("UNCHECKED_CAST", "TooManyFunctions")

package edu.illinois.cs.cs125.answerable.classdesignanalysis

import java.lang.IllegalStateException

const val NO_ERROR_MSG: String = "All good!"
/**
 * Turn a CDAMatcher into a nice message.
 *
 * For name, kind, modifier, type parameter, and superclass matchers, mismatches generate
 * a 3-line message of the form:
 * AnalysisType mismatch;
 * Expected: expected
 * Found:    actual
 *
 * Other matchers display an "expected" list and a "found" list, one item per line.
 */
// The intention here is only to be a sane default behavior, as with everything else in Answerable.
// If the user wants to produce their own error messages, the type, reference, and submission fields are all public.
val CDAMatcher<*>.message: String
    get() = when (this.type) {
        AnalysisType.NAME -> nameMismatchMessage(this as CDAMatcher<String>)
        AnalysisType.KIND -> kindMismatchMessage(this as CDAMatcher<ClassKind>)
        AnalysisType.MODIFIERS -> modifierMismatchMessage(this as CDAMatcher<List<String>>)
        AnalysisType.TYPE_PARAMS -> typeParamMismatchMessage(this as CDAMatcher<List<String>>)
        AnalysisType.SUPERCLASS -> superclassMismatchMessage(this as CDAMatcher<String?>)
        AnalysisType.INTERFACES -> interfaceMismatchMessage(this as CDAMatcher<List<String>>)
        AnalysisType.FIELDS -> fieldMismatchMessage(this as CDAMatcher<List<OssifiedField>>)
        AnalysisType.METHODS -> methodMismatchMessage(this as CDAMatcher<List<OssifiedExecutable>>)
        AnalysisType.INNER_CLASSES -> innerclassMismatchMessage(this as CDAMatcher<List<String>>)
    } ?: "${this.type.toString().capitalize()}: $NO_ERROR_MSG"

private fun <T> ifMismatched(matcher: CDAMatcher<T>, mkMsg: () -> String): String? =
    if (!matcher.match) mkMsg() else null

// kotlin is weird about allowing (T)->String to be instantiated by `toString` so a typical default parameter
// doesn't quite get us what we need.
private fun <T> mkSimpleMsg(matcher: CDAMatcher<T>, type: String, showFunc: ((T) -> String)? = null): String {
    val show: (T) -> String = showFunc ?: { t: T -> t.toString() }
    return formatSimpleMsg(type, show(matcher.reference), show(matcher.submission))
}
// A slightly more general way of creating messages that allows expected and actual to be formatted differently.
// Passing two functions as arguments works out to be a bit messier than just computing the message in place,
// but using a uniform formatting function keeps everything consistent. So this function and 'mkSimpleMsg'
// form a nice middle ground.
private fun formatSimpleMsg(type: String, expected: String, found: String): String =
    """
        |$type mismatch;
        |Expected: $expected
        |Found:    $found
    """.trimMargin()

private fun nameMismatchMessage(matcher: CDAMatcher<String>): String? = ifMismatched(matcher) {
    mkSimpleMsg(matcher, "Class name")
}

// Consideration for if/when config takes a LanguageMode: in JavaMode, stick to "enum"
// but in KotlinMode, use "enum class".
private fun ClassKind.asNoun() = when (this) {
    ClassKind.CLASS -> "a class"
    ClassKind.INTERFACE -> "an interface"
    ClassKind.ENUM -> "an enum class"
}
private fun kindMismatchMessage(matcher: CDAMatcher<ClassKind>): String? = ifMismatched(matcher) {
    mkSimpleMsg(matcher, "Class kind") { it.asNoun() }
}

private fun modifierMismatchMessage(matcher: CDAMatcher<List<String>>): String? = ifMismatched(matcher) {
    mkSimpleMsg(matcher, "Class modifiers") { it.joinToString(separator = " ") }
}

private fun typeParamMismatchMessage(matcher: CDAMatcher<List<String>>): String? = ifMismatched(matcher) {
    mkSimpleMsg(matcher, "Class type parameter") { list ->
        list.joinToString(prefix = "<", separator = ", ", postfix = ">")
    }
}

private fun superclassMismatchMessage(matcher: CDAMatcher<String?>): String? = ifMismatched(matcher) {
    val expected: String = if (matcher.reference == null) "No class to be extended" else "extends ${matcher.reference}"
    val found: String = if (matcher.submission == null) "No class was extended" else "extends ${matcher.submission}"
    return@ifMismatched formatSimpleMsg("Superclass", expected, found)
}

private fun interfaceMismatchMessage(matcher: CDAMatcher<List<String>>, kind: ClassKind = ClassKind.CLASS): String? =
    ifMismatched(matcher) {
        val verbRoot: String = if (kind == ClassKind.INTERFACE) "extend" else "implement"
        val verbS = "${verbRoot}s"
        val verbEd = "${verbRoot}ed"
        val expected: String =
            if (matcher.reference.isEmpty()) "No interfaces to be $verbEd"
            else "$verbS ${matcher.reference.joinToString(separator = ", ")}"
        val found: String =
            if (matcher.submission.isEmpty()) "No interfaces were $verbEd"
            else "$verbS ${matcher.submission.joinToString(separator = ", ")}"
        return@ifMismatched formatSimpleMsg("Interface", expected, found)
    }

private fun fieldMismatchMessage(matcher: CDAMatcher<List<OssifiedField>>): String? = ifMismatched(matcher) {
    mkPublicApiMismatchMsg(matcher.reference, matcher.submission, "field")
}
private fun methodMismatchMessage(matcher: CDAMatcher<List<OssifiedExecutable>>): String? = ifMismatched(matcher) {
    mkPublicApiMismatchMsg(matcher.reference, matcher.submission, "method")
}

private fun innerclassMismatchMessage(matcher: CDAMatcher<List<String>>): String? = ifMismatched(matcher) {
    mkPublicApiMismatchMsg(matcher.reference, matcher.submission, "inner class", plural = "es")
}

private fun <T> mkPublicApiMismatchMsg(
    allExpected: List<T>,
    allActual: List<T>,
    apiTypeName: String,
    plural: String = "s"
): String {
    fun String.plural(isPlural: Boolean) = if (isPlural) this + plural else this
    val exp = allExpected.filter { it !in allActual }
    val act = allActual.filter { it !in allExpected }

    return when (Pair(exp.isEmpty(), act.isEmpty())) {
        Pair(first = true, second = false) -> {
            val single = act.size == 1
            """
                    |Found ${if (single) "an " else ""}unexpected public ${apiTypeName.plural(!single)}:
                    |${act.joinToString(separator = "\n  ", prefix = "  ")}
                """.trimMargin()
        }
        Pair(first = false, second = true) -> {
            val single = exp.size == 1
            """
                    |Expected ${if (single) "another" else "more"} public ${apiTypeName.plural(!single)}:
                    |${exp.joinToString(separator = "\n  ", prefix = "  ")}
                """.trimMargin()
        }
        Pair(first = false, second = false) -> {
            // In this case the exact cause of the mismatch is undecidable
            // in the future we can try and provide better guesses here, but for now we'll dump everything
            // See MultiplyMismatched notes in Analyze.kt
            """
                    |Expected public ${apiTypeName.plural(exp.size != 1)}:
                    |${exp.joinToString(separator = "\n  ", prefix = "  ")}
                    |Found public ${apiTypeName.plural(act.size != 1)}:
                    |${act.joinToString(separator = "\n  ", prefix = "  ")}
                """.trimMargin()
        }
        else -> throw IllegalStateException(
            "Tried to generate API mismatch error, but no mismatch was found.\nPlease report a bug."
        )
    }
}
