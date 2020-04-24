package edu.illinois.cs.cs125.answerable.classdesignanalysis

import edu.illinois.cs.cs125.answerable.api.DefaultSerializable
import edu.illinois.cs.cs125.answerable.api.EdgeCase
import edu.illinois.cs.cs125.answerable.api.Generator
import edu.illinois.cs.cs125.answerable.api.Helper
import edu.illinois.cs.cs125.answerable.api.Ignore
import edu.illinois.cs.cs125.answerable.api.Next
import edu.illinois.cs.cs125.answerable.api.Precondition
import edu.illinois.cs.cs125.answerable.api.SimpleCase
import edu.illinois.cs.cs125.answerable.api.Solution
import edu.illinois.cs.cs125.answerable.api.Verify
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
fun classDesignAnalysis(
    reference: Class<*>, submission: Class<*>, config: CDAConfig = defaultCDAConfig
): CDAResult = TODO("Not finished refactoring class design analysis.")

// TODO: expose these functions like
// fun Class<*>.namesMatch(Class<*>): ClassDesignMatch<String>
// fun Class<*>.methodsMatch(Class<*>, String?): ClassDesignMatch<String>
//
// the runner function looks like //           vvvvvv contains the nullable string
// fun classDesignAnalysis(Class<*>, Class<*>, Config): ClassDesignAnalysisResult
class ClassDesignAnalysis(
    private val solutionName: String? = null,
    private val reference: Class<*>,
    private val attempt: Class<*>
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
        AnalysisType.NAME,
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
            AnalysisType.KIND,
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
            AnalysisType.MODIFIERS,
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
        AnalysisType.TYPE_PARAMS,
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
            AnalysisType.SUPERCLASSES,
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
            AnalysisType.FIELDS,
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
        AnalysisType.METHODS,
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

class CDAConfig(
    val checkName: Boolean = true,
    val checkKind: Boolean = true,
    val checkModifiers: Boolean = true,
    val checkTypeParams: Boolean = true,
    val checkSuperclasses: Boolean = true,
    val checkInterfaces: Boolean = true,
    val checkFields: Boolean = true,
    val checkMethods: Boolean = true,
    val checkInnerClasses: Boolean = true,
    val solutionName: String? = null
)
val defaultCDAConfig = CDAConfig()

// TODO: Move into ClassDesignMatch as part of refactor
enum class AnalysisType {
    NAME, KIND, MODIFIERS, TYPE_PARAMS, SUPERCLASSES, INTERFACES, INNER_CLASSES, FIELDS, METHODS;

    override fun toString(): String = name.toLowerCase().replace('_', ' ')
}

data class ClassDesignMatch<T>(
    val type: AnalysisType,
    val reference: T,
    val submission: T,
    val match: Boolean = reference == submission
)

/**
 * The result of a run of the class design analyzer.
 */
@Suppress("MemberVisibilityCanBePrivate") // This is part of the API so must be public for users to consume
class CDAResult(
    val configuration: CDAConfig,
    val names: ClassDesignMatch<String>?,
    val kinds: ClassDesignMatch<ClassKind>?,
    /* this should be serialized as a String representing the modifiers
       it contains, since an API consumer can't reliably access the
       Modifier static methods (:
     */
    val modifiers: ClassDesignMatch<Int>?,
    // order is significant since it affects the API
    val typeParams: ClassDesignMatch<List<String>>?,
    /* As much as it'd be nice to store the actual Type object, we want
       to support users who have students write both a parent AND child class
       in one assignment, particularly if it's in Kotlin. In this case, the
       reference and submission superclasses will be different Types!
     */
    val superclass: ClassDesignMatch<String>?,
    // see above
    val interfaces: ClassDesignMatch<List<String>>?,
    val fields: ClassDesignMatch<List<Field>>?, // match by answerableName
    val methods: ClassDesignMatch<List<Executable>>?, // match by answerableName.

    val innerClasses: ClassDesignMatch<List<String>>?,
    val innerClassAnalyses: Map<String, CDAResult>? // null if !innerClasses.match
) {
    val allMatch: Boolean
        get() = names?.match ?: true &&
                kinds?.match ?: true &&
                modifiers?.match ?: true &&
                typeParams?.match ?: true &&
                superclass?.match ?: true &&
                interfaces?.match ?: true &&
                fields?.match ?: true &&
                methods?.match ?: true &&
                innerClasses?.match ?: true &&
                innerClassAnalyses?.all { entry ->
                    entry.value.allMatch
                } ?: true
}

/* TODO: try this once everything else is working
potential idea like:
MutliplyMatchable<T>( come up with a better name
  val: List<T> // submission is missing
  val: List<T> // submission has but shouldn't have
  // in a naive world everything in here matched, but in a really really cool world in the future,
  // some of these things don't match but we've been able to guess that this is what the submission _meant_ to match.
  val: List<ClassDesignMatch<T>>

  val matched = list1 && list 2 are empty and everything in list 3 matches
 */

// Including this is low-cost over using strings and is easier to safely fix in the future if we, say,
// want to report abstract classes as a kind mismatch rather than a modifier mismatch.
enum class ClassKind {
    // enum classes can contain methods in Java
    CLASS, INTERFACE, ENUM;

    override fun toString(): String = name.toLowerCase()
}

val Class<*>.kind
    get() = when {
        this.isEnum -> ClassKind.ENUM
        this.isInterface -> ClassKind.INTERFACE
        else -> ClassKind.CLASS
    }

fun String.load(): Class<*> = Class.forName(this)

fun Type.simpleName() = this.typeName.split(".").last()
fun Field.simpleName() = this.name.split(".").last()
fun Field.answerableName() = "${Modifier.toString(this.modifiers)} ${this.type.simpleName()} ${this.simpleName()}"
fun Class<*>.publicFields(filter: (field: Field) -> Boolean = { true }) =
    this.getPublicFields().filter(filter)

fun Class<*>.publicMethods(filter: (executable: Executable) -> Boolean = { true }) =
    (this.getPublicMethods() + this.constructors).filter(filter)

// alias filterNot with a more sensible name
fun <T> Iterable<T>.filterOut(p: (T) -> Boolean) = this.filterNot(p)

class AnalysisOutput(val tag: AnalysisType, val result: AnalysisResult<*>) : DefaultSerializable {
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
