package edu.illinois.cs.cs125.answerable.typeManagement

import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType

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
                if (it.isEmpty()) "" else it.joinToString(prefix = "<", postfix = ">", transform = Type::simpleSourceName)
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
