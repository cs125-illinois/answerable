package edu.illinois.cs.cs125.answerable.testing

import edu.illinois.cs.cs125.answerable.ArrayWrapper

private val defaultIntSimpleCases = intArrayOf(-1, 1)
private val defaultByteSimpleCases = byteArrayOf(-1, 1)
private val defaultShortSimpleCases = shortArrayOf(-1, 1)
private val defaultLongSimpleCases = longArrayOf(-1, 1)
private val defaultDoubleSimpleCases = doubleArrayOf(-1.0, 1.0)
private val defaultFloatSimpleCases = floatArrayOf(-1f, 1f)
private val defaultCharSimpleCases = charArrayOf('a', 'A', '0')
private val defaultStringSimpleCases = arrayOf("a", "A", "0")
internal val valueSimpleCases = mapOf(
    Int::class.java to ArrayWrapper(defaultIntSimpleCases),
    Byte::class.java to ArrayWrapper(defaultByteSimpleCases),
    Short::class.java to ArrayWrapper(defaultShortSimpleCases),
    Long::class.java to ArrayWrapper(defaultLongSimpleCases),
    Double::class.java to ArrayWrapper(defaultDoubleSimpleCases),
    Float::class.java to ArrayWrapper(defaultFloatSimpleCases),
    Char::class.java to ArrayWrapper(defaultCharSimpleCases),
    String::class.java to ArrayWrapper(defaultStringSimpleCases)
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
    IntArray::class.java to ArrayWrapper(defaultIntArraySimpleCases),
    ByteArray::class.java to ArrayWrapper(defaultByteArraySimpleCases),
    ShortArray::class.java to ArrayWrapper(defaultShortArraySimpleCases),
    LongArray::class.java to ArrayWrapper(defaultLongArraySimpleCases),
    DoubleArray::class.java to ArrayWrapper(defaultDoubleArraySimpleCases),
    FloatArray::class.java to ArrayWrapper(defaultFloatArraySimpleCases),
    CharArray::class.java to ArrayWrapper(defaultCharArraySimpleCases),
    Array<String>::class.java to ArrayWrapper(defaultStringArraySimpleCases)
)

private val defaultIntEdgeCases = intArrayOf(0)
private val defaultDoubleEdgeCases = doubleArrayOf(0.0)
private val defaultFloatEdgeCases = floatArrayOf(0f)
private val defaultByteEdgeCases = byteArrayOf(0)
private val defaultShortEdgeCases = shortArrayOf(0)
private val defaultLongEdgeCases = longArrayOf(0)
private val defaultCharEdgeCases = charArrayOf(' ')
internal val defaultPrimitiveEdgeCases = mapOf(
    Int::class.java to ArrayWrapper(defaultIntEdgeCases),
    Byte::class.java to ArrayWrapper(defaultByteEdgeCases),
    Short::class.java to ArrayWrapper(defaultShortEdgeCases),
    Long::class.java to ArrayWrapper(defaultLongEdgeCases),
    Double::class.java to ArrayWrapper(defaultDoubleEdgeCases),
    Float::class.java to ArrayWrapper(defaultFloatEdgeCases),
    Char::class.java to ArrayWrapper(defaultCharEdgeCases),
    Boolean::class.java to ArrayWrapper(booleanArrayOf())
)
