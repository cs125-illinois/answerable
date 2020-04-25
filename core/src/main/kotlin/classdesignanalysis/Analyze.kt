package edu.illinois.cs.cs125.answerable.classdesignanalysis

import edu.illinois.cs.cs125.answerable.api.DefaultSerializable
import edu.illinois.cs.cs125.answerable.EdgeCase
import edu.illinois.cs.cs125.answerable.Generator
import edu.illinois.cs.cs125.answerable.Helper
import edu.illinois.cs.cs125.answerable.Ignore
import edu.illinois.cs.cs125.answerable.Next
import edu.illinois.cs.cs125.answerable.Precondition
import edu.illinois.cs.cs125.answerable.SimpleCase
import edu.illinois.cs.cs125.answerable.Solution
import edu.illinois.cs.cs125.answerable.Verify
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

fun Class<*>.namesMatch(other: Class<*>): ClassDesignMatch<String> =
    ClassDesignMatch(AnalysisType.NAME, this.simpleName, other.simpleName)

fun Class<*>.kindsMatch(other: Class<*>): ClassDesignMatch<ClassKind> =
    ClassDesignMatch(AnalysisType.KIND, this.kind, other.kind)

fun Class<*>.modifiersMatch(other: Class<*>): ClassDesignMatch<Int> =
    ClassDesignMatch(AnalysisType.MODIFIERS, this.modifiers, other.modifiers)

fun Class<*>.typeParametersMatch(other: Class<*>): ClassDesignMatch<List<String>> =
    ClassDesignMatch(
        AnalysisType.TYPE_PARAMS,
        this.typeParameters.map { it.simpleSourceName },
        other.typeParameters.map { it.simpleSourceName }
    )

/* Note: [Comparing superclasses and interface implementations]
 * Because an assignment could reasonably ask students to write _both_ the parent and child class,
 * we can't just compare the Type objects directly. Confirming that they have the same source
 * representation is enough for our needs.
 */
fun Class<*>.superclassesMatch(other: Class<*>): ClassDesignMatch<String> =
    ClassDesignMatch(
        AnalysisType.SUPERCLASSES,
        this.genericSuperclass.simpleSourceName,
        other.genericSuperclass.simpleSourceName
    )

fun Class<*>.interfacesMatch(other: Class<*>): ClassDesignMatch<List<String>> =
    ClassDesignMatch(
        AnalysisType.INTERFACES,
        this.genericInterfaces.map { it.simpleSourceName }.sorted(),
        other.genericInterfaces.map { it.simpleSourceName }.sorted()
    )

fun Class<*>.fieldsMatch(other: Class<*>): ClassDesignMatch<List<OssifiedField>> =
    ClassDesignMatch(
        AnalysisType.FIELDS,
        this.publicFields().map { OssifiedField(it) }.sortedBy { it.answerableName },
        other.publicFields().map { OssifiedField(it) }.sortedBy { it.answerableName }
    )

fun Class<*>.methodsMatch(other: Class<*>, solutionName: String?): ClassDesignMatch<List<OssifiedExecutable>> {
    val referenceAnnotations = setOf(
        Generator::class.java,
        Verify::class.java,
        Next::class.java,
        EdgeCase::class.java,
        SimpleCase::class.java,
        Precondition::class.java,
        Helper::class.java,
        Ignore::class.java
    )
    fun methodFilter(method: Executable) = referenceAnnotations.none { method.isAnnotationPresent(it) } &&
        !(method.getAnnotation(Solution::class.java)?.name?.let { it != solutionName } ?: false)

    return ClassDesignMatch(
        AnalysisType.METHODS,
        this.publicMethods(::methodFilter).map { OssifiedExecutable(it) }.sortedBy { it.answerableName },
        other.publicMethods(::methodFilter).map { OssifiedExecutable(it) }.sortedBy { it.answerableName }
    )
}

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
    val modifiers: ClassDesignMatch<List<String>>?,
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

class OssifiedField(field: Field) {
    val modifiers: List<String> = Modifier.toString(field.modifiers).split(" ")
    // note: formerly used field.type.simpleName, which fails for a generic field in a generic class
    val type: String = field.genericType.simpleSourceName
    val name: String = field.simpleName()

    val answerableName: String
        get() = (modifiers.toMutableList() + type + name).joinToString(separator = " ")

    override fun equals(other: Any?): Boolean {
        if (other !is OssifiedField) return false

        return answerableName == other.answerableName
    }

    override fun hashCode(): Int {
        var result = modifiers.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }
}

@Suppress("MemberVisibilityCanBePrivate")
class OssifiedExecutable(executable: Executable) {
    val isDefault: Boolean = executable is Method && executable.isDefault

    // we need to ignore transient because methods cannot be transient. It seems like that bit is being used
    // to mark that a method is varargs?
    // If you want to ignore a modifier, you have to filter it out of the list. Storing modifiers as an int
    // makes it harder to serialize, and I can't think of a practical reason to ignore modifiers in CDA.
    val modifiers: List<String> = Modifier.toString(
        executable.modifiers and Modifier.TRANSIENT.inv()
    ).split(" ")
    val typeParams: List<String> = executable.typeParameters.map { it.simpleSourceName }
    val returnType: String? = (executable as? Method)?.genericReturnType?.simpleSourceName
    val name: String = executable.simpleName()
    val parameters: List<String> = executable.genericParameterTypes.mapIndexed { index, type ->
        if (executable.isVarArgs && index == executable.genericParameterTypes.size - 1) {
            type.simpleSourceName.replaceFirst("\\[]$".toRegex(), "...")
        } else {
            type.simpleSourceName
        }
    }
    val throws: List<String> = executable.genericExceptionTypes.map { it.simpleSourceName }

    val answerableName: String
        get() {
            val parts: MutableList<String> = mutableListOf()
            if (isDefault) {
                parts.add("default")
            }
            parts.add(modifiers.joinToString(separator = " "))
            if (typeParams.isNotEmpty()) {
                parts.add(typeParams.joinToString(prefix = "<", separator = ", ", postfix = ">"))
            }
            returnType?.let { parts.add(it) }
            // don't parts.add(name) or else you get <name> () instead of <name>()
            parts.add(name + parameters.joinToString(prefix = "(", separator = ", ", postfix = ")"))
            if (throws.isNotEmpty()) {
                parts.add(throws.joinToString(prefix = "throws ", separator = ", "))
            }
            return parts.joinToString(separator = " ")
        }

    override fun equals(other: Any?): Boolean {
        if (other !is OssifiedExecutable) return false

        return answerableName == other.answerableName
    }

    override fun hashCode(): Int {
        var result = isDefault.hashCode()
        result = 31 * result + modifiers.hashCode()
        result = 31 * result + typeParams.hashCode()
        result = 31 * result + (returnType?.hashCode() ?: 0)
        result = 31 * result + name.hashCode()
        result = 31 * result + parameters.hashCode()
        result = 31 * result + throws.hashCode()
        return result
    }
}

fun String.load(): Class<*> = Class.forName(this)

fun Class<*>.publicFields(filter: (field: Field) -> Boolean = { true }) =
    this.getPublicFields().filter(filter)

fun Class<*>.publicMethods(filter: (executable: Executable) -> Boolean = { true }) =
    (this.getPublicMethods() + this.constructors).filter(filter)

// alias filterNot with a more sensible name
// TODO: can toss this after refactor
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

fun Type.simpleName() = this.typeName.split(".").last()
fun Executable.simpleName() = this.name.split(".").last()
fun Executable.answerableName() = OssifiedExecutable(this).answerableName
fun Field.simpleName() = this.name.split(".").last()
fun Field.answerableName() = OssifiedField(this).answerableName

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
