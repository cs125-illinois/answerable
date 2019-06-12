package edu.illinois.cs.cs125.answerable.typeManagement

import edu.illinois.cs.cs125.answerable.*
import edu.illinois.cs.cs125.answerable.ArrayWrapper
import edu.illinois.cs.cs125.answerable.ByteArrayWrapper
import edu.illinois.cs.cs125.answerable.IntArrayWrapper
import edu.illinois.cs.cs125.answerable.LongArrayWrapper
import edu.illinois.cs.cs125.answerable.ShortArrayWrapper
import java.lang.IllegalStateException
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

internal fun wrapArray(arr: Any?): ArrayWrapper = when (arr) {
    null -> throw IllegalStateException()
    is IntArray -> IntArrayWrapper(arr)
    is ByteArray -> ByteArrayWrapper(arr)
    is ShortArray -> ShortArrayWrapper(arr)
    is LongArray -> LongArrayWrapper(arr)
    is FloatArray -> FloatArrayWrapper(arr)
    is DoubleArray -> DoubleArrayWrapper(arr)
    is CharArray -> CharArrayWrapper(arr)
    is BooleanArray -> BooleanArrayWrapper(arr)
    else -> AnyArrayWrapper(arr as Array<*>)
}