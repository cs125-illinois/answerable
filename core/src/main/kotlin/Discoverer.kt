@file: Suppress("TooManyFunctions", "SpreadOperator")

package edu.illinois.cs.cs125.answerable

import edu.illinois.cs.cs125.answerable.annotations.EdgeCase
import edu.illinois.cs.cs125.answerable.annotations.Generator
import edu.illinois.cs.cs125.answerable.annotations.Next
import edu.illinois.cs.cs125.answerable.annotations.SimpleCase
import edu.illinois.cs.cs125.answerable.annotations.Solution
import edu.illinois.cs.cs125.answerable.annotations.UseGenerator
import edu.illinois.cs.cs125.answerable.annotations.Verify
import edu.illinois.cs.cs125.answerable.typeManagement.correspondsTo
import edu.illinois.cs.cs125.answerable.typeManagement.sourceName
import java.lang.IllegalStateException
import java.lang.reflect.Field
import java.lang.reflect.GenericArrayType
import java.lang.reflect.Member
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Type

fun String.load(): Class<*> = Class.forName(this)

internal fun Class<*>.getReferenceSolutionMethod(name: String = ""): Method? {
    return this.declaredMethods.filter {
        it.getAnnotation(Solution::class.java)?.name?.equals(name) ?: false
    }.let { methods ->
        if (methods.isEmpty()) {
            return null
        } else if (methods.size > 1) {
            throw AnswerableMisuseException("Can't find singular solution method with tag `$name'.")
        }
        methods.first().also { it.isAccessible = true }
    }
}

internal fun Method.isPrinter(): Boolean = this.getAnnotation(Solution::class.java)?.prints ?: false

@Suppress("ThrowsCount")
internal fun Class<*>.findSolutionAttemptMethod(matchTo: Method?, correspondingClass: Class<*>): Method? {
    if (matchTo == null) return null
    val matchName: String = matchTo.name
    val matchRType: Type = matchTo.genericReturnType
    val matchPTypes: Array<Type> = matchTo.genericParameterTypes

    var methods = this.declaredMethods.filter { it.name == matchName }
    if (methods.isEmpty()) {
        throw SubmissionMismatchException("Expected a method named `$matchName'.")
    }

    methods = methods.filter { matchRType.correspondsTo(it.genericReturnType, correspondingClass, this) }
    if (methods.isEmpty()) {
        throw SubmissionMismatchException("Expected a method with return type `${matchRType.sourceName}'.")
    }

    methods = methods.filter { m ->
        m.genericParameterTypes.size == matchPTypes.size &&
            (matchPTypes.indices).all {
                matchPTypes[it].correspondsTo(
                    m.genericParameterTypes[it],
                    correspondingClass,
                    this
                )
            }
    }
    if (methods.isEmpty()) {
        throw SubmissionMismatchException(
            "Expected a method with parameter types `${matchPTypes.joinToString(
                prefix = "[",
                postfix = "]"
            ) { it.sourceName }}'."
        )
    }
    // If the student code compiled, there can only be one method
    return methods[0].also { it.isAccessible = true }
}

internal val Class<*>.publicFields: List<Field>
    get() = declaredFields.filter { Modifier.isPublic(it.modifiers) }

internal val Class<*>.publicMethods: List<Method>
    get() = declaredMethods.filter { Modifier.isPublic(it.modifiers) }

internal val Class<*>.publicInnerClasses: List<Class<*>>
    get() = declaredClasses.filter { Modifier.isPublic(it.modifiers) }

internal fun Class<*>.getAllGenerators(): List<Method> =
    this.declaredMethods
        .filter { method -> method.isAnnotationPresent(Generator::class.java) }
        .map { it.isAccessible = true; it }

internal fun Class<*>.getEnabledGenerators(enabledNames: Array<String>): List<Method> =
    this.declaredMethods
        .filter { it.isAnnotationPresent(Generator::class.java) }
        .groupBy { it.genericReturnType }
        .flatMap { entry ->
            when (entry.value.size) {
                1 -> entry.value
                else -> {
                    entry.value
                        .filter { it.getAnnotation(Generator::class.java).name in enabledNames }
                        .let { enabledGenerators ->
                            when (enabledGenerators.size) {
                                1 -> enabledGenerators
                                else -> {
                                    val name = entry.key.sourceName
                                    throw AnswerableMisuseException(
                                        "Failed to resolve @Generator conflict:\n" +
                                            "Multiple enabled generators found for type `$name'."
                                    )
                                }
                            }
                        }
                }
            }
        }
        .map { it.isAccessible = true; it }

internal fun Method.getAnswerableParams(): Array<Pair<Type, String?>> =
    this.parameters.map { Pair(it.parameterizedType, it.getAnnotation(UseGenerator::class.java)?.name) }.toTypedArray()

internal fun Class<*>.getDefaultAtNext(): Method? =
    this.declaredMethods
        .filter { it.getAnnotation(Next::class.java)?.name?.equals("") ?: false }
        .let { unnamedNexts ->
            when (unnamedNexts.size) {
                0 -> null
                1 -> unnamedNexts[0].also { it.isAccessible = true }
                else -> throw AnswerableMisuseException(
                    "Failed to resolve @Next conflict:\nFound multiple unnamed @Next annotations."
                )
            }
        }

internal fun Class<*>.getAtNext(enabledNames: Array<String>): Method? =
    this.declaredMethods
        .filter { it.getAnnotation(Next::class.java)?.name?.let { name -> enabledNames.contains(name) } ?: false }
        .let { atNexts ->
            when (atNexts.size) {
                0 -> null
                1 -> atNexts[0].also { it.isAccessible = true }
                else -> throw AnswerableMisuseException(
                    "Failed to resolve @Next conflict:\nFound multiple @Next annotations with enabled names."
                )
            }
        }
        ?: this.getDefaultAtNext()

internal fun Class<*>.getCustomVerifier(name: String): Method? =
    this.declaredMethods
        .filter { it.getAnnotation(Verify::class.java)?.name?.equals(name) ?: false }
        .let { verifiers ->
            when (verifiers.size) {
                0 -> null
                1 -> verifiers[0].also { it.isAccessible = true }
                else -> throw AnswerableMisuseException("Found multiple @Verify annotations with name `$name'.")
            }
        }

// We use this class so that we can manipulate Fields and Methods at the same time,
// as it is an erroneous conflict to provide both a field and a method for the same type's edge or corner cases.
private class FieldOrMethod(val member: Member) {
    init {
        when (member) {
            is Field -> member.isAccessible = true
            is Method -> member.isAccessible = true
        }
    }

    val genericType: Type = when (member) {
        is Field -> member.genericType
        is Method -> member.genericReturnType
        else -> throw IllegalStateException()
    }

    fun <T : Annotation> isAnnotationPresent(annotationClass: Class<T>): Boolean = when (member) {
        is Field -> member.isAnnotationPresent(annotationClass)
        is Method -> member.isAnnotationPresent(annotationClass)
        else -> throw IllegalStateException()
    }

    fun <T : Annotation> getAnnotation(annotationClass: Class<T>): T? = when (member) {
        is Field -> member.getAnnotation(annotationClass)
        is Method -> member.getAnnotation(annotationClass)
        else -> throw IllegalStateException()
    }

    fun get(): ArrayWrapper {
        val value = when (member) {
            is Field -> member[null]
            is Method -> member.invoke(null)
            else -> throw IllegalStateException()
        } ?: throw IllegalStateException()

        return ArrayWrapper(value)
    }
}

@Suppress("ComplexMethod", "LongMethod", "ThrowsCount")
private fun Class<*>.getEnabledCases(edgeIfElseSimple: Boolean, enabledNames: Array<String>): Map<Type, ArrayWrapper> {
    val annotationClass = if (edgeIfElseSimple) EdgeCase::class.java else SimpleCase::class.java

    val caseName = if (edgeIfElseSimple) "edge" else "simple"

    val declaredMembers: List<Member> = this.declaredFields.toList() + this.declaredMethods.toList()
    val declaredFMs: List<FieldOrMethod> = declaredMembers.map { FieldOrMethod(it) }

    val casesInUseList = declaredFMs
        .filter { it.isAnnotationPresent(annotationClass) }
        .map {
            if (it.genericType.let { type -> (type is Class<*> && type.isArray) || type is GenericArrayType }) {
                it
            } else {
                throw AnswerableMisuseException("${caseName.capitalize()} case providers must provide an array.")
            }
        }
        .groupBy {
            it.genericType.let { type ->
                when (type) {
                    is Class<*> -> type.componentType
                    is GenericArrayType -> type.genericComponentType
                    else -> throw IllegalStateException()
                }
            } as Type
        }
        .mapValues { entry ->
            when (entry.value.size) {
                0 -> throw IllegalStateException("An error occurred after a `groupBy` call. Please report a bug.")
                1 -> entry.value[0]
                else ->
                    entry.value
                        .filter {
                            when (val annotation = it.getAnnotation(annotationClass)) {
                                is EdgeCase? -> annotation?.name
                                is SimpleCase? -> annotation?.name
                                else -> null
                            } in enabledNames
                        }
                        .let { enabled ->
                            when (enabled.size) {
                                0 -> entry.value.find {
                                    when (val annotation = it.getAnnotation(annotationClass)) {
                                        is EdgeCase? -> annotation?.name
                                        is SimpleCase? -> annotation?.name
                                        else -> null
                                    }?.equals("") ?: false
                                }
                                1 -> enabled[0]
                                else ->
                                    throw AnswerableMisuseException(
                                        "Multiple enabled $caseName case providers for type `${entry.key.sourceName}'."
                                    )
                            }
                        }
            }
        }
        .mapNotNull { if (it.value == null) null else Pair(it.key, it.value as FieldOrMethod) }

    val casesInUseMap = mapOf(*casesInUseList.toTypedArray())

    return casesInUseMap.mapValues {
        try {
            return@mapValues it.value.get()
        } catch (e: IllegalStateException) {
            throw AnswerableMisuseException("Provided $caseName cases array for type `${it.key.sourceName}' was null.")
        }
    }
}

internal fun Class<*>.getEnabledEdgeCases(enabledNames: Array<String>): Map<Type, ArrayWrapper> =
    getEnabledCases(true, enabledNames)

internal fun Class<*>.getEnabledSimpleCases(enabledNames: Array<String>): Map<Type, ArrayWrapper> =
    getEnabledCases(false, enabledNames)
