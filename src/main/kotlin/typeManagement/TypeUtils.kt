package edu.illinois.cs.cs125.answerable.typeManagement

import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType

internal fun Type.sourceName(): String = when (this) {
    is Class<*> -> this.canonicalName
    is GenericArrayType -> "${this.genericComponentType.sourceName()}[]"
    is ParameterizedType ->
        "${this.rawType.sourceName()}${this.actualTypeArguments.let {
            if (it.isEmpty()) "" else it.joinToString(prefix = "<", postfix = ">", transform = Type::sourceName)
        }}"
    is WildcardType -> {
        when {
            this.lowerBounds.isNotEmpty() -> "? super ${lowerBounds[0].sourceName()}"
            this.upperBounds.isNotEmpty() ->
                "? extends ${upperBounds.joinToString(separator = " & ", transform = Type::sourceName)}"
            else -> "?"
        }
    }
    else -> this.toString()
}

internal fun boxArray(arr: Any?): Array<*> = when (arr) {
    is IntArray -> arr.toTypedArray()
    is ByteArray -> arr.toTypedArray()
    is ShortArray -> arr.toTypedArray()
    is LongArray -> arr.toTypedArray()
    is FloatArray -> arr.toTypedArray()
    is DoubleArray -> arr.toTypedArray()
    is CharArray -> arr.toTypedArray()
    is BooleanArray -> arr.toTypedArray()
    else -> arr as Array<*>
}