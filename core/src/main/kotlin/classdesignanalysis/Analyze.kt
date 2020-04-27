@file:Suppress("TooManyFunctions")

package edu.illinois.cs.cs125.answerable.classdesignanalysis

import edu.illinois.cs.cs125.answerable.annotations.DEFAULT_EMPTY_NAME
import edu.illinois.cs.cs125.answerable.annotations.EdgeCase
import edu.illinois.cs.cs125.answerable.annotations.Generator
import edu.illinois.cs.cs125.answerable.annotations.Helper
import edu.illinois.cs.cs125.answerable.annotations.Ignore
import edu.illinois.cs.cs125.answerable.annotations.Next
import edu.illinois.cs.cs125.answerable.annotations.Precondition
import edu.illinois.cs.cs125.answerable.annotations.SimpleCase
import edu.illinois.cs.cs125.answerable.annotations.Solution
import edu.illinois.cs.cs125.answerable.annotations.Verify
import edu.illinois.cs.cs125.answerable.api.DefaultSerializable
import edu.illinois.cs.cs125.answerable.api.defaultToJson
import edu.illinois.cs.cs125.answerable.getPublicFields
import edu.illinois.cs.cs125.answerable.getPublicMethods
import edu.illinois.cs.cs125.answerable.typeManagement.simpleSourceName
import java.lang.reflect.Executable
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Type

// TODO: analyze inner classes recursively

/**
 * Analyzer that determines that a submission class is equivalent in design to a reference class.
 *
 * The reference class for a question defines a "contract," a specification of the public members that any
 * implementation must expose. Answerable's job is to ascertain that a submission meets the specification both on the
 * surface, in API, and behaviorally. This is the API component.
 */
data class ClassDesignMatch(
    val type: Type,
    val reference: String,
    var other: String,
    val matched: Boolean = reference == other
) {
    enum class Type {
        Names, Types, Modifiers, TypeParameters, Parents, Interfaces, Fields, Methods
    }
}

fun String.load(): Class<*> = Class.forName(this)

fun Class<*>.namesMatch(other: Class<*>) = ClassDesignMatch(
    ClassDesignMatch.Type.Names, this.simpleName, other.simpleName
)

fun Class<*>.type() = if (this.isInterface) {
    "interface"
} else {
    "class"
}

fun Class<*>.typesMatch(other: Class<*>) = ClassDesignMatch(
    ClassDesignMatch.Type.Types, this.type(), other.type()
)

fun Class<*>.modifiersMatch(other: Class<*>) = ClassDesignMatch(
    ClassDesignMatch.Type.Modifiers, Modifier.toString(this.modifiers), Modifier.toString(other.modifiers)
)

fun Class<*>.typeParameters() = this.typeParameters.joinToString(separator = ", ") { it.name }
fun Class<*>.typeParametersMatch(other: Class<*>) = ClassDesignMatch(
    ClassDesignMatch.Type.TypeParameters,
    this.typeParameters(),
    other.typeParameters()
)

fun Class<*>.parentsMatch(other: Class<*>) = ClassDesignMatch(
    ClassDesignMatch.Type.Parents,
    this.genericSuperclass.toString(),
    other.genericSuperclass.toString()
)

fun Class<*>.interfaces() = this.genericInterfaces
    .map { it.typeName }
    .sorted()
    .joinToString(separator = ", ")

fun Class<*>.interfacesMatch(other: Class<*>) = ClassDesignMatch(
    ClassDesignMatch.Type.Interfaces,
    this.interfaces(),
    other.interfaces()
)

fun Type.simpleName() = this.typeName.split(".").last()
fun Field.simpleName() = this.name.split(".").last()
fun Field.answerableName() = "${Modifier.toString(this.modifiers)} ${this.type.simpleName()} ${this.simpleName()}"
fun Class<*>.publicFields(filter: (field: Field) -> Boolean = { true }) = this.getPublicFields()
    .filter(filter)
    .map { it.answerableName() }
    .sorted()
    .joinToString(separator = ", ")

fun Class<*>.publicFieldsMatch(other: Class<*>, filter: (field: Field) -> Boolean = { true }) =
    ClassDesignMatch(
        ClassDesignMatch.Type.Fields,
        this.publicFields(filter),
        other.publicFields(filter)
    )

fun Class<*>.publicMethods(filter: (executable: Executable) -> Boolean = { true }) =
    (this.getPublicMethods() + this.constructors)
        .filter(filter)
        .map { it.answerableName() }
        .sorted()
        .joinToString(separator = ", ")

fun Class<*>.publicMethodsMatch(other: Class<*>, filter: (executable: Executable) -> Boolean = { true }) =
    ClassDesignMatch(
        ClassDesignMatch.Type.Methods,
        this.publicMethods(filter),
        other.publicMethods(filter)
    )

class ClassDesignAnalysis(
    private val reference: Class<*>,
    private val attempt: Class<*>,
    private val solutionName: String = DEFAULT_EMPTY_NAME
) {
    @Suppress("LongParameterList")
    fun runSuite(
        name: Boolean = true,
        classStatus: Boolean = true,
        classModifiers: Boolean = true,
        typeParams: Boolean = true,
        superClasses: Boolean = true,
        fields: Boolean = true,
        methods: Boolean = true
    ): List<AnalysisOutput> {

        val output = mutableListOf<AnalysisOutput>()
        if (name) output.add(namesMatch())
        if (classStatus) output.add(classStatusMatch())
        if (classModifiers) output.add(classModifiersMatch())
        if (typeParams) output.add(typeParamsMatch())
        if (superClasses) output.add(superClassesMatch())
        if (fields) output.add(publicFieldsMatch())
        if (methods) output.add(publicMethodsMatch())

        return output
    }

    private fun namesMatch() = AnalysisOutput(
        AnalysisTag.NAME,
        if (reference.simpleName == attempt.simpleName) {
            Matched(reference.simpleName)
        } else {
            Mismatched(
                reference.simpleName,
                attempt.simpleName
            )
        }
    )

    private fun classStatusMatch() =
        AnalysisOutput(
            AnalysisTag.STATUS,
            if (reference.isInterface == attempt.isInterface) {
                Matched(if (reference.isInterface) "interface" else "class")
            } else {
                val expected = if (reference.isInterface) "interface" else "class"
                val actual = if (attempt.isInterface) "interface" else "class"
                Mismatched(expected, actual)
            }
        )

    private fun classModifiersMatch() =
        AnalysisOutput(
            AnalysisTag.MODIFIERS,
            if (reference.modifiers == attempt.modifiers) {
                Matched(
                    Modifier.toString(
                        reference.modifiers
                    )
                )
            } else {

                val expected = Modifier.toString(reference.modifiers)
                val actual = Modifier.toString(attempt.modifiers)

                Mismatched(expected, actual)
            }
        )

    // Order is significant since it's part of the class API
    fun typeParamsMatch() = AnalysisOutput(
        AnalysisTag.TYPE_PARAMS,
        run {
            val refTypes = reference.typeParameters.map { it.name }
            val attTypes = attempt.typeParameters.map { it.name }
            return@run if (refTypes == attTypes) {
                Matched(refTypes)
            } else {
                Mismatched(refTypes, attTypes)
            }
        })

    // Order is not significant here
    private fun superClassesMatch() =
        AnalysisOutput(
            AnalysisTag.SUPERCLASSES,
            if (reference.genericSuperclass == attempt.genericSuperclass &&
                reference.genericInterfaces contentEquals attempt.genericInterfaces
            ) {
                Matched(
                    Pair(
                        reference.genericSuperclass,
                        reference.genericInterfaces
                    )
                )
            } else {
                Mismatched(
                    Pair(reference.genericSuperclass, reference.genericInterfaces),
                    Pair(attempt.genericSuperclass, attempt.genericInterfaces)
                )
            }
        )

    private val referenceAnnotations = setOf(
        Generator::class.java,
        Verify::class.java,
        Next::class.java,
        EdgeCase::class.java,
        SimpleCase::class.java,
        Precondition::class.java,
        Helper::class.java,
        Ignore::class.java
    )

    private fun publicFieldsMatch() =
        AnalysisOutput(
            AnalysisTag.FIELDS,
            run {
                fun mkFieldString(field: Field): String {
                    val sb = StringBuilder()
                    printModifiersIfNonzero(
                        sb,
                        field.modifiers,
                        false
                    )
                    sb.append(field.type.simpleName).append(" ")
                    sb.append(field.simpleName())
                    return sb.toString()
                }

                // order is irrelevant; we only care about the API
                val refFields =
                    reference.getPublicFields()
                        .filter { field -> referenceAnnotations.none { field.isAnnotationPresent(it) } }
                        .map(::mkFieldString)
                        .sorted()
                val attFields =
                    attempt.getPublicFields()
                        .map(::mkFieldString)
                        .sorted()

                return@run if (refFields == attFields) {
                    Matched(refFields)
                } else {
                    Mismatched(
                        refFields,
                        attFields
                    )
                }
            })

    fun publicMethodsMatch() = AnalysisOutput(
        AnalysisTag.METHODS,
        run {
            val refMethodData =
                (reference.getPublicMethods() + reference.constructors)
                    .filter { method -> referenceAnnotations.none { method.isAnnotationPresent(it) } }
                    .filterOut { method ->
                        method.getAnnotation(Solution::class.java)?.name?.let { it != solutionName } ?: false
                    }
                    .map { it.answerableName() }
                    .sorted()
            val attMethodData =
                (attempt.getPublicMethods() + attempt.constructors)
                    .map { it.answerableName() }
                    .sorted()

            // if everything matched, then we're done.
            return@run if (refMethodData == attMethodData) {
                Matched(refMethodData)
            } else {
                Mismatched(
                    refMethodData,
                    attMethodData
                )
            }
        })
}

enum class AnalysisTag {
    NAME, STATUS, MODIFIERS, TYPE_PARAMS, SUPERCLASSES, FIELDS, METHODS, INNER_CLASSES;

    override fun toString(): String = name.toLowerCase().replace('_', ' ')
}

// alias filterNot with a more sensible name
fun <T> Iterable<T>.filterOut(p: (T) -> Boolean) = this.filterNot(p)

class AnalysisOutput(val tag: AnalysisTag, val result: AnalysisResult<*>) : DefaultSerializable {
    override fun toString() = this.toErrorMsg() // see ClassDesignErrors.kt
    override fun toJson() = this.defaultToJson()
}

fun List<AnalysisOutput>.toJson() = this.joinToString(prefix = "[", postfix = "]") { it.toJson() }

sealed class AnalysisResult<out T> : DefaultSerializable {
    override fun toJson() = this.defaultToJson()
}

data class Matched<T>(val found: T) : AnalysisResult<T>()
data class Mismatched<T>(val expected: T, val found: T) : AnalysisResult<T>()

fun Executable.answerableName(
    ignoredModifiers: Int = Modifier.TRANSIENT
): String {
    val parts = mutableListOf<String>()
    if (this is Method && this.isDefault) {
        parts.add("default")
    }
    parts.add(Modifier.toString(this.modifiers and ignoredModifiers.inv()))
    if (this.typeParameters.isNotEmpty()) {
        parts.add(
            this.typeParameters.joinToString(
                prefix = "<",
                separator = ", ",
                postfix = ">"
            ) { it.simpleSourceName }
        )
    }
    if (this is Method) {
        parts.add(this.genericReturnType.simpleSourceName)
    }
    parts.add(
        this.genericParameterTypes.mapIndexed { index, type ->
            if (this.isVarArgs && index == this.genericParameterTypes.size - 1) {
                type.simpleSourceName.replaceFirst("\\[]$".toRegex(), "...")
            } else {
                type.simpleSourceName
            }
        }.joinToString(prefix = "${this.name.split(".").last()}(", separator = ", ", postfix = ")")
    )
    if (this.genericExceptionTypes.isNotEmpty()) {
        parts.add(
            this.genericExceptionTypes.joinToString(prefix = "throws ", separator = ", ") {
                it.simpleSourceName
            }
        )
    }
    return parts.joinToString(separator = " ")
}

private fun printModifiersIfNonzero(sb: StringBuilder, mask: Int, isDefault: Boolean) {
    var mod = mask

    if (mod != 0 && !isDefault) {
        sb.append(Modifier.toString(mod)).append(' ')
    } else {
        val accessModifiers = Modifier.PUBLIC or Modifier.PROTECTED or Modifier.PRIVATE
        val accessMod = mod and (accessModifiers) // infix bitwise &
        if (accessMod != 0)
            sb.append(Modifier.toString(accessMod)).append(' ')
        if (isDefault)
            sb.append("default ")
        mod = mod and accessModifiers.inv()
        if (mod != 0)
            sb.append(Modifier.toString(mod)).append(' ')
    }
}
