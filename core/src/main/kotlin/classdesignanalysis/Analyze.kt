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
import edu.illinois.cs.cs125.answerable.publicConstructors
import edu.illinois.cs.cs125.answerable.publicFields
import edu.illinois.cs.cs125.answerable.publicInnerClasses
import edu.illinois.cs.cs125.answerable.publicMethods
import edu.illinois.cs.cs125.answerable.simpleSourceName
import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Type

/*
 * For the purposes of analysis, all Types are compared by name as the name appears in the source code.
 * This is fine for using Answerable as a "I have one class written by a student that I want to test" system.
 * However, especially in Kotlin mode, a submission could define its own classes, with the same names as the
 * intended ones, use them instead, and pass analysis. This could potentially cause nasty things to happen.
 *
 * We should strongly consider switching to doing all matching via qualified names, and strip the qualifiers
 * in the toErrorMessage implementation for the various matchers when the qualifiers agree and the simpleName doesn't.
 * CDA could be extended in the future to relax such a check via annotations.
 */
/**
 * Analyzer that determines that a [submission] class is equivalent in design to a [reference] class.
 *
 * The reference class for a question defines a "contract," a specification of the public members that any
 * implementation must expose. Answerable's job is to ascertain that a submission meets the specification both on the
 * surface, in API, and behaviorally. This is the API component.
 *
 * The analysis proceeds by analyzing several distinct components: names, kinds, modifiers, type parameters,
 * superclass (or lack thereof), implemented interfaces, fields, and methods. Inner classes are analyzed recursively.
 * Components can be individually disabled via the [config].
 */
fun classDesignAnalysis(reference: Class<*>, submission: Class<*>, config: CDAConfig = defaultCDAConfig): CDAResult {
    val nameEnv = config.nameEnv?.copy(
        referenceName = config.nameEnv.referenceName ?: reference.simpleSourceName
    )

    val innerClassesResult: InnerClassAnalysisResult? =
        runIf(config.checkInnerClasses) {
            reference.innerClassesMatch(
                submission,
                config.copy(nameEnv = nameEnv)
            )
        }
    return CDAResult(
        config = config,
        names = runIf(config.checkName) { reference.namesMatch(submission, nameEnv) },
        kinds = runIf(config.checkKind) { reference.kindsMatch(submission) },
        modifiers = runIf(config.checkModifiers) { reference.modifiersMatch(submission) },
        typeParams = runIf(config.checkTypeParams) { reference.typeParametersMatch(submission) },
        superclass = runIf(config.checkSuperclasses) { reference.superclassesMatch(submission, nameEnv) },
        interfaces = runIf(config.checkInterfaces) { reference.interfacesMatch(submission, nameEnv) },
        fields = runIf(config.checkFields) { reference.fieldsMatch(submission, nameEnv) },
        methods = runIf(config.checkMethods) {
            reference.methodsMatch(submission, config.solutionName, nameEnv)
        },
        constructors = runIf(config.checkConstructors) {
            reference.constructorsMatch(submission, nameEnv)
        },
        innerClasses = innerClassesResult?.innerClassNames,
        innerClassAnalyses = innerClassesResult?.recursiveResults
    )
}

internal fun <T> runIf(condition: Boolean, block: () -> T): T? =
    if (condition) block() else null

/**
 * Used to support differently-named submission classes.
 *
 * When matching type names, if the reference refers to itself as a type, it should be matched
 * as though it referred to the submission's type instead. CDANameEnv represents this substituation.
 *
 * If [referenceName] is null, the name of the class which the xxxxMatch method was called on will be used.
 */
data class CDANameEnv(
    val expectedSubmissionName: String,
    val referenceName: String? = null
)

/**
 * Factors out the most common use case of [changeTypeName].
 *
 * If [cdaNameEnv] is null, the string is returned unchanged. Otherwise, if [cdaNameEnv] contains a
 * referenceName, that referenceName is replaced with the expected submission name. Otherwise,
 * the simpleName of [clazz] is used as if it were the referenceName.
 */
private fun <T> T.changeTypeName(clazz: Class<*>, cdaNameEnv: CDANameEnv?, ctn: (T, String, String) -> T): T =
    if (cdaNameEnv == null) {
        this
    } else {
        ctn(this, cdaNameEnv.referenceName ?: clazz.simpleSourceName, cdaNameEnv.expectedSubmissionName)
    }
internal fun String.changeTypeName(clazz: Class<*>, cdaNameEnv: CDANameEnv?): String =
    this.changeTypeName(clazz, cdaNameEnv, String::changeTypeName)
internal fun OssifiedField.changeTypeName(clazz: Class<*>, cdaNameEnv: CDANameEnv?): OssifiedField =
    this.changeTypeName(clazz, cdaNameEnv, OssifiedField::changeTypeName)
internal fun OssifiedExecutable.changeTypeName(clazz: Class<*>, cdaNameEnv: CDANameEnv?): OssifiedExecutable =
    this.changeTypeName(clazz, cdaNameEnv, OssifiedExecutable::changeTypeName)

/**
 * Component analyzer. Checks the names of the classes, as they appear in the source.
 */
fun Class<*>.namesMatch(other: Class<*>, cdaNameEnv: CDANameEnv? = null): CDAMatcher<String> =
    CDAMatcher(
        AnalysisType.NAME,
        this.simpleSourceName.changeTypeName(this, cdaNameEnv),
        other.simpleSourceName
    )

/**
 * Component analyzer. Checks the kinds of the classes. A class's kind is either
 * class, interface, or enum.
 */
fun Class<*>.kindsMatch(other: Class<*>): CDAMatcher<ClassKind> =
    CDAMatcher(AnalysisType.KIND, this.kind, other.kind)

/**
 * Component analyzer. Checks the modifiers on the classes, such as final and abstract.
 *
 * When analyzing Kotlin classes, 'open' is realized as no modifier, and a lack of 'open' is realized
 * as a 'final' modifier.
 */
fun Class<*>.modifiersMatch(other: Class<*>): CDAMatcher<List<String>> =
    CDAMatcher(
        AnalysisType.MODIFIERS,
        Modifier.toString(this.modifiers).split(" "),
        Modifier.toString(other.modifiers).split(" ")
    )

/*
 * The order of type parameters affects the API of a class when used in source code, but Answerable
 * proper will still function if this check is disabled. However, we don't want to mention this in the KDoc
 * because Answerable proper doesn't expose the CDAConfig that will pass to CDA as part of the pipeline.
 */
/**
 * Component analyzer. Checks the type parameters of the classes, by name, as they appear in the source.
 */
fun Class<*>.typeParametersMatch(other: Class<*>): CDAMatcher<List<String>> =
    CDAMatcher(
        AnalysisType.TYPE_PARAMS,
        this.typeParameters.map { it.simpleSourceName },
        other.typeParameters.map { it.simpleSourceName }
    )

/*
 * Because an assignment could reasonably ask students to write _both_ the parent and child class,
 * we can't just compare the Type objects directly. Confirming that they have the same source
 * representation is enough for our needs, just as we compare the class source names.
 *
 * addendum: see https://cs125-forum.cs.illinois.edu/t/answerable-worklog/13896/224
 */
/**
 * Component analyzer. Checks that the classes inherit from superclasses of the same name, as they appear in the
 * source.
 *
 * All classes have a superclass. If one is not explicitly extended, it implicitly extends Object. For our purposes,
 * an explicit extension of Object and an implicit one are indistinguishable, and we don't really care. If a class
 * extends Object in /any/ way, it is reflected by null.
 *
 * Interfaces also have a null superclass, even if they extend other interfaces. Such extensions
 * are reflected by [interfacesMatch].
 */
fun Class<*>.superclassesMatch(other: Class<*>, cdaNameEnv: CDANameEnv? = null): CDAMatcher<String?> {
    // It would make sense to instead leave "Object" in the matcher itself, and only strip it in messages for
    // the purpose of not having "extends Object" in the message (which can be confusing for beginners).
    // Doing it this way is a design decision, so if people complain about it in the future, change it!
    // Nothing ties us down to doing it this way.
    fun unlessObject(type: Type?): Type? =
        if (type == Object().javaClass) null else type

    return CDAMatcher(
        AnalysisType.SUPERCLASS,
        unlessObject(this.genericSuperclass)?.simpleSourceName?.changeTypeName(this, cdaNameEnv),
        unlessObject(other.genericSuperclass)?.simpleSourceName
    )
}

/**
 * Component analyzer. Checks that the classes implement the same set of interfaces, as the names
 * of the interfaces appear in the source. The interfaces can be implemented in any order.
 */
fun Class<*>.interfacesMatch(other: Class<*>, cdaNameEnv: CDANameEnv? = null): CDAMatcher<List<String>> =
    CDAMatcher(
        AnalysisType.INTERFACES,
        this.genericInterfaces.map { it.simpleSourceName.changeTypeName(this, cdaNameEnv) }.sorted(),
        other.genericInterfaces.map { it.simpleSourceName }.sorted()
    )

/**
 * Component analyzer. Checks that the classes have the same public fields, by name and type.
 * The type is checked by name, as it appears in the source.
 */
fun Class<*>.fieldsMatch(other: Class<*>, cdaNameEnv: CDANameEnv? = null): CDAMatcher<List<OssifiedField>> {

    fun fieldFilter(field: Field) = referenceAnnotations.none { field.isAnnotationPresent(it) }

    return CDAMatcher(
        AnalysisType.FIELDS,
        this.publicFields(::fieldFilter)
            .map { OssifiedField(it).changeTypeName(this, cdaNameEnv) }
            .sortedBy { it.answerableName },
        other.publicFields(::fieldFilter).map(::OssifiedField).sortedBy { it.answerableName }
    )
}

/**
 * Component analyzer. Checks that the classes have the same public methods, by name,
 * return type, type parameters, and parameter types. All types are checked by name, as
 * they appear in the source.
 */
fun Class<*>.methodsMatch(
    other: Class<*>,
    solutionName: String = DEFAULT_EMPTY_NAME,
    nameEnv: CDANameEnv? = null
): CDAMatcher<List<OssifiedExecutable>> {
    fun methodFilter(method: Executable) = referenceAnnotations.none { method.isAnnotationPresent(it) } &&
        !(method.getAnnotation(Solution::class.java)?.name?.let { it != solutionName } ?: false)

    return CDAMatcher(
        AnalysisType.METHODS,
        this.publicMethods(::methodFilter)
            .map { OssifiedExecutable(it).changeTypeName(this, nameEnv) }
            .sortedBy { it.answerableName },
        other.publicMethods(::methodFilter).map(::OssifiedExecutable).sortedBy { it.answerableName }
    )
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

/**
 * Component analyzer. Checks that classes contain the same constructors. Constructors are compared
 * the same way methods are compared. However,
 */
fun Class<*>.constructorsMatch(other: Class<*>, cdaNameEnv: CDANameEnv? = null): CDAMatcher<List<OssifiedExecutable>> =
    CDAMatcher(
        AnalysisType.CONSTRUCTORS,
        this.publicConstructors()
            .map { OssifiedExecutable(it).changeTypeName(this, cdaNameEnv) }
            .sortedBy { it.answerableName },
        other.publicConstructors().map(::OssifiedExecutable).sortedBy { it.answerableName }
    )

// You can't currently put the solution method /inside/ an inner class
// for Answerable's purposes but this incidentally supports it for the purposes of CDA.
/**
 * Component analyzer. Consists of two parts.
 *
 * Part 1 checks that the classes contain inner classes of the same name, as the names appear in the source.
 * Part 2 recursively analyzes inner classes of the same name. Currently, Answerable does not attempt to
 * recurse on /any/ inner classes if it cannot match all of the names, and an empty map is returned.
 * This should be considered an implementation detail and is subject to change in the future.
 *
 * @param [config] can be used to adjust which analyses are run on the inner classes.
 * @return Results from both parts are returned as a tuple.
 */
fun Class<*>.innerClassesMatch(
    other: Class<*>,
    config: CDAConfig = defaultCDAConfig
): InnerClassAnalysisResult {
    // We want to use the names as seen in the source. Currently, 'simpleSourceName' simply delegates
    // to 'simpleName' for classes, but this could feasibly change in the future
    // (for example, to gather a qualified name instead).
    val ourInnerClasses: List<Class<*>> = this.publicInnerClasses.sortedBy { it.simpleSourceName }
    val theirInnerClasses: List<Class<*>> = other.publicInnerClasses.sortedBy { it.simpleSourceName }
    val nameMatcher: CDAMatcher<List<String>> =
        CDAMatcher(
            AnalysisType.INNER_CLASSES,
            ourInnerClasses.map { it.simpleSourceName },
            theirInnerClasses.map { it.simpleSourceName }
        )

    if (!nameMatcher.match) return InnerClassAnalysisResult(nameMatcher, mapOf())

    val recursiveAnalysis: List<Pair<String, CDAResult>> =
        ourInnerClasses.zip(theirInnerClasses).map { (ours, theirs) ->
            ours.simpleSourceName to classDesignAnalysis(ours, theirs, config)
        }

    return InnerClassAnalysisResult(nameMatcher, recursiveAnalysis.toMap())
}

data class InnerClassAnalysisResult(
    val innerClassNames: CDAMatcher<List<String>>,
    val recursiveResults: Map<String, CDAResult>
)

@Suppress("LongParameterList")
data class CDAConfig(
    val checkName: Boolean = true,
    val checkKind: Boolean = true,
    val checkModifiers: Boolean = true,
    val checkTypeParams: Boolean = true,
    val checkSuperclasses: Boolean = true,
    val checkInterfaces: Boolean = true,
    val checkFields: Boolean = true,
    val checkMethods: Boolean = true,
    val checkConstructors: Boolean = true,
    val checkInnerClasses: Boolean = true,

    /**
     * Used to support differently-named solutions. [referenceName] should be the name of
     * the outermost reference class. `null` can be used instead when analyzing the outermost class.
     */
    val nameEnv: CDANameEnv? = null,
    val solutionName: String = DEFAULT_EMPTY_NAME
)
val defaultCDAConfig = CDAConfig()

// This was temporarily an inner class of CDAMatcher but it created unnecessarily verbose code and
// it didn't seem like it belonged there. This can be moved in the future with no real damage. 5/26/20
enum class AnalysisType {
    NAME, KIND, MODIFIERS, TYPE_PARAMS, SUPERCLASS, INTERFACES, INNER_CLASSES, FIELDS, METHODS, CONSTRUCTORS;

    override fun toString(): String = name.toLowerCase().replace('_', ' ')
}

data class CDAMatcher<T>(
    val type: AnalysisType,
    val reference: T,
    val submission: T
) {
    val match: Boolean by lazy { reference == submission }

    override fun toString(): String = this.message
}

/**
 * The result of a run of the class design analyzer.
 */
@Suppress("MemberVisibilityCanBePrivate") // This is part of the API so must be public for users to consume
data class CDAResult(
    val config: CDAConfig,
    val names: CDAMatcher<String>?,
    val kinds: CDAMatcher<ClassKind>?,
    val modifiers: CDAMatcher<List<String>>?,
    // order is significant since it affects the API
    val typeParams: CDAMatcher<List<String>>?,
    /* As much as it'd be nice to store the actual Type object, we want
       to support users who have students write both a parent AND child class
       in one assignment, particularly if it's in Kotlin. In this case, the
       reference and submission superclasses will be different Types!
     */
    val superclass: CDAMatcher<String?>?,
    // see above
    val interfaces: CDAMatcher<List<String>>?,
    val fields: CDAMatcher<List<OssifiedField>>?, // match by answerableName
    val methods: CDAMatcher<List<OssifiedExecutable>>?, // match by answerableName
    val constructors: CDAMatcher<List<OssifiedExecutable>>?, // match by answerableName

    val innerClasses: CDAMatcher<List<String>>?,
    val innerClassAnalyses: Map<String, CDAResult>? // empty if !innerClasses.match, null if !config.checkInnerClasses
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
            constructors?.match ?: true &&
            innerClasses?.match ?: true &&
            innerClassAnalyses?.all { entry ->
            entry.value.allMatch
        } ?: true

    private fun getMessages(include: (Boolean?) -> Boolean): List<String> {
        val messages: MutableList<String> = mutableListOf()
        fun addMessage(matcher: CDAMatcher<*>) {
            if (include(matcher.match)) messages.add(matcher.message)
        }
        this.names?.also(::addMessage)
        this.kinds?.also(::addMessage)
        this.modifiers?.also(::addMessage)
        this.typeParams?.also(::addMessage)
        this.superclass?.also(::addMessage)
        this.interfaces?.also(::addMessage)
        this.fields?.also(::addMessage)
        this.methods?.also(::addMessage)
        this.constructors?.also(::addMessage)
        this.innerClasses?.also(::addMessage)
        this.innerClassAnalyses?.values?.forEach { res ->
            messages.addAll(res.getMessages(include))
        }
        return messages
    }

    /**
     * Produces a flat list of nice error messages for the mismatches
     * in the result. Includes the mismatches in the inner classes.
     * Messages are only included for mismatches.
     */
    val errorMessages: List<String>
        get() = getMessages { match -> match != null && !match }
    /**
     * Produces a flat list of nice messages for all matchers in the result.
     * Includes messages in the inner classes.
     */
    val messages: List<String>
        get() = getMessages { true }
}

val noCDAResult: CDAResult = classDesignAnalysis(
    Any().javaClass, // just need two classes, they won't be inspected
    Any().javaClass,
    CDAConfig(
        checkName = false,
        checkKind = false,
        checkModifiers = false,
        checkTypeParams = false,
        checkFields = false,
        checkMethods = false,
        checkConstructors = false,
        checkSuperclasses = false,
        checkInterfaces = false,
        checkInnerClasses = false
    )
)

/* Here's an idea to consider:
MutliplyMatchable<T>(
  val: List<T> // submission is missing
  val: List<T> // submission has but shouldn't have
  // in a naive world everything in here matched, but in a really really cool world in the future,
  // some of these things don't match but we've been able to guess that this is what the submission _meant_ to match.
  val: List<ClassDesignMatch<T>>

  val matched = list1 && list 2 are empty and everything in list 3 matches
 */

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

class OssifiedField private constructor(
    val modifiers: List<String>,
    val type: String,
    val name: String
) {
    constructor(field: Field) : this(
        modifiers = Modifier.toString(field.modifiers).split(" "),
        // note: formerly used field.type.simpleName, which fails for a generic field in a generic class
        type = field.genericType.simpleSourceName,
        name = field.simpleName()
    )

    internal fun changeTypeName(from: String, to: String): OssifiedField = OssifiedField(
        modifiers = modifiers,
        type = type.changeTypeName(from, to),
        name = name
    )

    val answerableName: String
        get() = (modifiers.toMutableList() + type + name).joinToString(separator = " ")

    override fun toString(): String = answerableName

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

@Suppress("MemberVisibilityCanBePrivate", "LongParameterList")
data class OssifiedExecutable(
    val isConstructor: Boolean,
    val isDefault: Boolean,
    val modifiers: List<String>,
    val typeParams: List<String>,
    val returnType: String?,
    val name: String,
    val parameters: List<String>,
    val throws: List<String>
) {
    constructor(executable: Executable) : this(
        isConstructor = executable is Constructor<*>,
        isDefault = executable is Method && executable.isDefault,
        // we need to ignore transient because methods cannot be transient. It seems like that bit is being used
        // to mark that a method is varargs?
        // If you want to ignore a modifier, you have to filter it out of the list. Storing modifiers as an int
        // makes it harder to serialize, and I can't think of a practical reason to ignore modifiers in CDA.
        modifiers = Modifier.toString(
            executable.modifiers and Modifier.TRANSIENT.inv()
        ).split(" "),

        typeParams = executable.typeParameters.map { it.simpleSourceName },
        returnType = (executable as? Method)?.genericReturnType?.simpleSourceName,
        name = executable.simpleName(),
        parameters = executable.genericParameterTypes.mapIndexed { index, type ->
            if (executable.isVarArgs && index == executable.genericParameterTypes.size - 1) {
                type.simpleSourceName.replaceFirst("\\[]$".toRegex(), "...")
            } else {
                type.simpleSourceName
            }
        },
        throws = executable.genericExceptionTypes.map { it.simpleSourceName }
    )

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

    internal fun changeTypeName(from: String, to: String): OssifiedExecutable = this.copy(
        typeParams = typeParams.map { it.changeTypeName(from, to) },
        returnType = returnType?.changeTypeName(from, to),
        name = if (isConstructor) name.changeTypeName(from, to) else name,
        parameters = parameters.map { it.changeTypeName(from, to) },
        throws = throws.map { it.changeTypeName(from, to) }
    )

    override fun toString(): String = answerableName

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

fun Type.simpleName() = this.typeName.split(".").last()
fun Executable.simpleName() = this.name.split(".").last()
fun Executable.answerableName() = OssifiedExecutable(this).answerableName
fun Field.simpleName() = this.name.split(".").last()
fun Field.answerableName() = OssifiedField(this).answerableName

/**
 * Finds all instances of the type name [from] in [this] and replaces them with [to].
 *
 * Sometimes, this can be tricky. For example, "YourBinaryTree".changeTypeName("BinaryTree", "Submission")
 * must return "YourBinaryTree", not "YourSubmission".
 */
fun String.changeTypeName(from: String, to: String): String {
    fun isIsolatedTypeName(match: MatchResult): Boolean {
        val matchStart = match.range.first
        val matchEnd = match.range.last
        val isStartOfName = matchStart == 0 || !Character.isJavaIdentifierPart(this[matchStart - 1])
        val isEndOfName = matchEnd == this.length - 1 || !Character.isJavaIdentifierPart(this[matchEnd + 1])
        return isStartOfName && isEndOfName
    }
    return this.replace(from.toRegex()) { mr ->
        if (isIsolatedTypeName(mr)) {
            to
        } else {
            from
        }
    }
}
