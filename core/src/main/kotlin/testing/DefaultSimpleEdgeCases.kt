package edu.illinois.cs.cs125.answerable.testing

import edu.illinois.cs.cs125.answerable.WrappedArray

private val defaultIntSimpleCases = intArrayOf(-1, 1)
private val defaultByteSimpleCases = byteArrayOf(-1, 1)
private val defaultShortSimpleCases = shortArrayOf(-1, 1)
private val defaultLongSimpleCases = longArrayOf(-1, 1)
private val defaultDoubleSimpleCases = doubleArrayOf(-1.0, 1.0)
private val defaultFloatSimpleCases = floatArrayOf(-1f, 1f)
private val defaultCharSimpleCases = charArrayOf('a', 'A', '0')
private val defaultStringSimpleCases = arrayOf("a", "A", "0")
internal val valueSimpleCases = mapOf(
    Int::class.java to WrappedArray(defaultIntSimpleCases),
    Byte::class.java to WrappedArray(defaultByteSimpleCases),
    Short::class.java to WrappedArray(defaultShortSimpleCases),
    Long::class.java to WrappedArray(defaultLongSimpleCases),
    Double::class.java to WrappedArray(defaultDoubleSimpleCases),
    Float::class.java to WrappedArray(defaultFloatSimpleCases),
    Char::class.java to WrappedArray(defaultCharSimpleCases),
    String::class.java to WrappedArray(defaultStringSimpleCases)
)

private val defaultIntArraySimpleCases = arrayOf(intArrayOf(0))
private val defaultByteArraySimpleCases = arrayOf(byteArrayOf(0))
private val defaultShortArraySimpleCases = arrayOf(shortArrayOf(0))
private val defaultLongArraySimpleCases = arrayOf(longArrayOf(0))
private val defaultDoubleArraySimpleCases = arrayOf(doubleArrayOf(0.0))
private val defaultFloatArraySimpleCases = arrayOf(floatArrayOf(0f))
private val defaultCharArraySimpleCases = arrayOf(charArrayOf(' '))
private val defaultStringArraySimpleCases = arrayOf(arrayOf(""))
internal val arraySimpleCases = mapOf(
    IntArray::class.java to WrappedArray(defaultIntArraySimpleCases),
    ByteArray::class.java to WrappedArray(defaultByteArraySimpleCases),
    ShortArray::class.java to WrappedArray(defaultShortArraySimpleCases),
    LongArray::class.java to WrappedArray(defaultLongArraySimpleCases),
    DoubleArray::class.java to WrappedArray(defaultDoubleArraySimpleCases),
    FloatArray::class.java to WrappedArray(defaultFloatArraySimpleCases),
    CharArray::class.java to WrappedArray(defaultCharArraySimpleCases),
    Array<String>::class.java to WrappedArray(defaultStringArraySimpleCases)
)

private val defaultIntEdgeCases = intArrayOf(0)
private val defaultDoubleEdgeCases = doubleArrayOf(0.0)
private val defaultFloatEdgeCases = floatArrayOf(0f)
private val defaultByteEdgeCases = byteArrayOf(0)
private val defaultShortEdgeCases = shortArrayOf(0)
private val defaultLongEdgeCases = longArrayOf(0)
private val defaultCharEdgeCases = charArrayOf(' ')
internal val defaultPrimitiveEdgeCases = mapOf(
    Int::class.java to WrappedArray(defaultIntEdgeCases),
    Byte::class.java to WrappedArray(defaultByteEdgeCases),
    Short::class.java to WrappedArray(defaultShortEdgeCases),
    Long::class.java to WrappedArray(defaultLongEdgeCases),
    Double::class.java to WrappedArray(defaultDoubleEdgeCases),
    Float::class.java to WrappedArray(defaultFloatEdgeCases),
    Char::class.java to WrappedArray(defaultCharEdgeCases),
    Boolean::class.java to WrappedArray(booleanArrayOf())
)
