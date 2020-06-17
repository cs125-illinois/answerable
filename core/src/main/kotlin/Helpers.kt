package edu.illinois.cs.cs125.answerable

import java.lang.reflect.GenericArrayType
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType

internal fun Method.isStatic() = Modifier.isStatic(modifiers)

internal val Type.sourceName: String
    get() = when (this) {
        is Class<*> -> this.canonicalName
        is GenericArrayType -> "${this.genericComponentType.sourceName}[]"
        is ParameterizedType ->
            "${this.rawType.sourceName}${this.actualTypeArguments.let {
                if (it.isEmpty()) "" else it.joinToString(prefix = "<", postfix = ">", transform = Type::sourceName)
            }}"
        is WildcardType -> {
            when {
                this.lowerBounds.isNotEmpty() -> "? super ${lowerBounds[0].sourceName}"
                this.upperBounds.isNotEmpty() ->
                    "? extends ${upperBounds.joinToString(separator = " & ", transform = Type::sourceName)}"
                else -> "?"
            }
        }
        else -> this.toString()
    }

internal val Type.simpleSourceName: String
    get() = when (this) {
        is Class<*> -> this.simpleName
        is GenericArrayType -> "${this.genericComponentType.simpleSourceName}[]"
        is ParameterizedType ->
            "${this.rawType.simpleSourceName}${this.actualTypeArguments.let {
                if (it.isEmpty()) {
                    ""
                } else {
                    it.joinToString(prefix = "<", postfix = ">", transform = Type::simpleSourceName)
                }
            }}"
        is WildcardType -> {
            when {
                this.lowerBounds.isNotEmpty() -> "? super ${lowerBounds[0].simpleSourceName}"
                this.upperBounds.isNotEmpty() ->
                    "? extends ${upperBounds.joinToString(separator = " & ", transform = Type::simpleSourceName)}"
                else -> "?"
            }
        }
        else -> this.toString()
    }

@Suppress("ComplexMethod")
internal fun Type.correspondsTo(other: Type, mapFrom: Class<*>, mapTo: Class<*>): Boolean {
    if (this == other) return true
    return when (this) {
        is Class<*> -> this == mapFrom && other == mapTo
        is GenericArrayType ->
            other is GenericArrayType &&
                this.genericComponentType.correspondsTo(other.genericComponentType, mapFrom, mapTo)
        is ParameterizedType ->
            other is ParameterizedType &&
                this.rawType.correspondsTo(other.rawType, mapFrom, mapTo) &&
                this.actualTypeArguments.size == other.actualTypeArguments.size &&
                this.actualTypeArguments.indices.all {
                    this.actualTypeArguments[it].correspondsTo(other.actualTypeArguments[it], mapFrom, mapTo)
                }
        is WildcardType ->
            other is WildcardType &&
                this.lowerBounds.size == other.lowerBounds.size &&
                this.upperBounds.size == other.upperBounds.size &&
                this.lowerBounds.indices.all {
                    this.lowerBounds[it].correspondsTo(other.lowerBounds[it], mapFrom, mapTo)
                } &&
                this.upperBounds.indices.all {
                    this.upperBounds[it].correspondsTo(other.upperBounds[it], mapFrom, mapTo)
                }
        else -> false
    }
}

internal fun String.slashName(): String = replace('.', '/')
internal fun String.dotName(): String = replace('/', '.')

/** Replace qualified class name dots with forward slashes to match format in .class files. */
internal fun Class<*>.slashName() = name.slashName()
fun String.load(): Class<*> = Class.forName(this)
