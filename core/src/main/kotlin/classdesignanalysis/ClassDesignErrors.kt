@file:Suppress("UNCHECKED_CAST", "TooManyFunctions")

package edu.illinois.cs.cs125.answerable.classdesignanalysis

import java.lang.IllegalStateException
import java.lang.reflect.Type

// TODO: goals for the rewrite here:
//   (1) provide a toErrorMessage function for each type of component mismatch
//   (2) Keep things private, because everything relies on the invariant that
//       the argument is actually a mismatch.
//   (3) Use the components to define CDAMatcher.toErrorMessage() here
//   (4) Use the components to define CDAResult.toErrorMessage() in Analyze.kt



internal fun AnalysisOutput.toErrorMsg() = when (this.result) {
    is Matched -> "Everything looks good!"
    is Mismatched -> when (this.tag) {
        AnalysisType.NAME -> ::mkNameError
        AnalysisType.KIND -> ::mkStatusError
        AnalysisType.MODIFIERS -> ::mkModifierError
        AnalysisType.TYPE_PARAMS -> ::mkTypeParamError
        AnalysisType.SUPERCLASS -> ::mkSuperClassError
        AnalysisType.INTERFACES -> TODO("Haven't implemented separate interface/superclass analysis")
        AnalysisType.INNER_CLASSES -> TODO("Haven't implemented recursive CDA.")
        AnalysisType.FIELDS -> ::mkFieldError
        AnalysisType.METHODS -> ::mkMethodError
    }(this.result)
}

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
    } ?: "${this.type.toString().capitalize()}: All good!"

private fun <T> ifMismatched(matcher: CDAMatcher<T>, mkMsg: () -> String): String? =
    if (matcher.match) mkMsg() else null

private fun nameMismatchMessage(matcher: CDAMatcher<String>): String? = ifMismatched(matcher) {
    """
        |Class name mismatch;
        |Expected: ${matcher.reference}
        |Found:    ${matcher.submission}
    """.trimIndent()
}


// Consideration for if/when config takes a LanguageMode: in JavaMode, stick to "enum"
// but in KotlinMode, use "enum class".
private fun ClassKind.asNoun() = when (this) {
    ClassKind.CLASS -> "a class"
    ClassKind.INTERFACE -> "an interface"
    ClassKind.ENUM -> "an enum class"
}
private fun kindMismatchMessage(matcher: CDAMatcher<ClassKind>): String? = ifMismatched(matcher) {
    """
        |Class kind mismatch;
        |Expected: ${matcher.reference.asNoun()}
        |Found:    ${matcher.submission.asNoun()}
    """.trimIndent()
}

private fun modifierMismatchMessage(matcher: CDAMatcher<List<String>>): String? = ifMismatched(matcher) {
    """
        |Class modifiers mismatch;
        |Expected: ${matcher.reference.joinToString(separator = " ")}
        |Found:    ${matcher.submission.joinToString(separator = " ")}
    """.trimMargin()
}

private fun typeParamMismatchMessage(matcher: CDAMatcher<List<String>>): String? = ifMismatched(matcher) {
    """
        |Type parameter mismatch;
        |Expected: ${matcher.reference.joinToString(prefix = "<", separator = ", ", postfix = ">")}
        |Found:    ${matcher.submission.joinToString(prefix = "<", separator = ", ", postfix = ">")}
    """.trimIndent()
}

private fun superclassMismatchMessage(matcher: CDAMatcher<String?>): String? = ifMismatched(matcher) {
    val expected: String = if (matcher.reference == null) "No class to be extended" else "extends ${matcher.reference}"
    val found: String = if (matcher.submission == null) "No class was extended" else "extends ${matcher.submission}"
    return@ifMismatched """
        |Superclass mismatch;
        |Expected: $expected
        |Found:    $found
    """.trimIndent()
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
        return@ifMismatched """
            |Interface mismatch;
            |Expected: $expected
            |Found:    $found
        """.trimIndent()
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
    return "Expected type parameter${if (singleE) "" else "s"} : " +
        "${expected.joinToString(prefix = "<", postfix = ">")}\n" +
        "Found type parameter${if (singleA) "" else "s"}$spaces: " +
        actual.joinToString(prefix = "<", postfix = ">")
}

private fun mkSuperClassError(result: Mismatched<*>): String {
    result as Mismatched<Pair<Type?, Array<out Type>>>

    return mkSuperclassMismatchErrorMsg(
        result.expected.first,
        result.expected.second,
        result.found.first,
        result.found.second
    )
}

private fun mkSuperclassMismatchErrorMsg(
    referenceSC: Type?,
    referenceI: Array<out Type>,
    attemptSC: Type?,
    attemptI: Array<out Type>
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
        .joinToString(separator, prefix, postfix = "", transform = transform) +
        last + safeTransform(this.last()) + postfix)
}

private fun mkFieldError(result: Mismatched<*>): String {
    result as Mismatched<List<*>>

    return mkPublicApiMismatchMsg(
        result.expected,
        result.found,
        "field"
    )
}

private fun mkMethodError(result: Mismatched<*>): String {
    result as Mismatched<List<*>>

    return mkPublicApiMismatchMsg(
        result.expected,
        result.found,
        "method"
    )
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
            // In this case the exact cause of the mismatch is provably undecidable
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
