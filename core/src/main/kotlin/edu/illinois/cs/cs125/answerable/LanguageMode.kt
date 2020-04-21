package edu.illinois.cs.cs125.answerable

import edu.illinois.cs.cs125.answerable.typeManagement.TypePool
import edu.illinois.cs.cs125.answerable.typeManagement.getDefiningKotlinFileClass
import java.util.*
import kotlin.math.min
import java.lang.reflect.Array as ReflectArray

internal interface LanguageMode {
    val defaultSimpleCases: Map<Class<*>, ArrayWrapper>
    val defaultEdgeCases: Map<Class<*>, ArrayWrapper>
    val defaultGenerators: Map<Class<*>, Gen<*>>
    fun findControlClass(clazz: Class<*>, typePool: TypePool): Class<*>?
}

private val defaultIntSimpleCases = intArrayOf(-1, 1)
private val defaultByteSimpleCases = byteArrayOf(-1, 1)
private val defaultShortSimpleCases = shortArrayOf(-1, 1)
private val defaultLongSimpleCases = longArrayOf(-1, 1)
private val defaultDoubleSimpleCases = doubleArrayOf(-1.0, 1.0)
private val defaultFloatSimpleCases = floatArrayOf(-1f, 1f)
private val defaultCharSimpleCases = charArrayOf('a', 'A', '0')
private val defaultStringSimpleCases = arrayOf("a", "A", "0")
private val valueSimpleCases = mapOf(
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
private val arraySimpleCases = mapOf(
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
private val defaultPrimitiveEdgeCases = mapOf(
        Int::class.java to ArrayWrapper(defaultIntEdgeCases),
        Byte::class.java to ArrayWrapper(defaultByteEdgeCases),
        Short::class.java to ArrayWrapper(defaultShortEdgeCases),
        Long::class.java to ArrayWrapper(defaultLongEdgeCases),
        Double::class.java to ArrayWrapper(defaultDoubleEdgeCases),
        Float::class.java to ArrayWrapper(defaultFloatEdgeCases),
        Char::class.java to ArrayWrapper(defaultCharEdgeCases),
        Boolean::class.java to ArrayWrapper(booleanArrayOf())
)

internal val defaultIntGen = object : Gen<Int> {
    override fun generate(complexity: Int, random: Random): Int {
        var comp = complexity
        if (complexity > Int.MAX_VALUE / 2) {
            comp = Int.MAX_VALUE / 2
        }
        return random.nextInt(comp * 2 + 1) - comp
    }
}

internal val defaultDoubleGen = object : Gen<Double> {
    override fun generate(complexity: Int, random: Random): Double {
        val denom = random.nextDouble() * (1e10 - 1) + 1
        val num = (random.nextDouble() * 2 * complexity * denom) - complexity * denom
        return num / denom
    }
}

internal val defaultFloatGen = object : Gen<Float> {
    override fun generate(complexity: Int, random: Random): Float {
        val denom = random.nextDouble() * (1e10 - 1) + 1
        val num = (random.nextDouble() * 2 * complexity * denom) - complexity * denom
        return (num / denom).toFloat() // if complexity is > 1e38, this stops being uniform
    }
}

internal val defaultByteGen = object : Gen<Byte> {
    override fun generate(complexity: Int, random: Random): Byte {
        return (random.nextInt(complexity * 2 + 1) - complexity).toByte()
    }
}

internal val defaultShortGen = object : Gen<Short> {
    override fun generate(complexity: Int, random: Random): Short {
        return (random.nextInt(complexity * 2 + 1) - complexity).toShort()
    }
}

internal val defaultLongGen = object : Gen<Long> {
    // see Random.nextInt(int) algorithm.
    private fun Random.nextLong(bound: Long): Long {
        var bits: Long
        var value: Long
        do {
            bits = (nextLong() shl 1) shr 1
            value = bits % bound
        } while (bits - value + (bound - 1) < 0L)
        return value
    }

    override fun generate(complexity: Int, random: Random): Long {
        return random.nextLong(complexity.toLong() * 4 + 1) - (complexity.toLong() * 2)
    }
}

internal val defaultCharGen = object : Gen<Char> {
    private fun Char.isPrintableAscii(): Boolean = this.toInt() in 32..126

    private fun Char.isPrint(): Boolean = isPrintableAscii() || Character.UnicodeBlock.of(this) in setOf(
            Character.UnicodeBlock.CYRILLIC, Character.UnicodeBlock.CYRILLIC_SUPPLEMENTARY, Character.UnicodeBlock.TAMIL, 
            Character.UnicodeBlock.CURRENCY_SYMBOLS, Character.UnicodeBlock.ARROWS, Character.UnicodeBlock.SUPPLEMENTAL_ARROWS_A,
            Character.UnicodeBlock.ETHIOPIC_EXTENDED, Character.UnicodeBlock.CJK_RADICALS_SUPPLEMENT, 
            Character.UnicodeBlock.KANGXI_RADICALS, Character.UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS,
            Character.UnicodeBlock.ENCLOSED_CJK_LETTERS_AND_MONTHS, Character.UnicodeBlock.OLD_PERSIAN
    )

    override fun generate(complexity: Int, random: Random): Char {
        return if (random.nextDouble() < min(.15/32 * complexity, .15)) {
            var char: Char
            do {
                char = random.nextInt(0x10000).toChar()
            } while (!char.isPrint())
            char
        } else {
            (random.nextInt(95) + 32).toChar()
        }
    }
}

internal val defaultAsciiGen = object : Gen<Char> {
    override fun generate(complexity: Int, random: Random): Char {
        return (random.nextInt(95) + 32).toChar()
    }
}

internal val defaultBooleanGen = object : Gen<Boolean> {
    override fun generate(complexity: Int, random: Random): Boolean = random.nextBoolean()
}

private val primitiveGenerators: Map<Class<*>, Gen<*>> = mapOf(
        Int::class.java     to defaultIntGen,
        Double::class.java  to defaultDoubleGen,
        Float::class.java   to defaultFloatGen,
        Byte::class.java    to defaultByteGen,
        Short::class.java   to defaultShortGen,
        Long::class.java    to defaultLongGen,
        Char::class.java    to defaultCharGen,
        Boolean::class.java to defaultBooleanGen
)

internal object JavaMode : LanguageMode {
    override val defaultSimpleCases: Map<Class<*>, ArrayWrapper>
        get() = valueSimpleCases + arraySimpleCases
    override val defaultEdgeCases: Map<Class<*>, ArrayWrapper>
        get() = (defaultPrimitiveEdgeCases + mapOf(String::class.java to ArrayWrapper(arrayOf(null, "")))).let {
            it + it.map { (clazz, _) ->
                val emptyArray = ReflectArray.newInstance(clazz, 0)
                emptyArray.javaClass to ArrayWrapper(arrayOf(emptyArray, null))
            }
        }
    override val defaultGenerators: Map<Class<*>, Gen<*>>
        get() = primitiveGenerators
    override fun findControlClass(clazz: Class<*>, typePool: TypePool): Class<*>?
            = null
}

internal object KotlinMode : LanguageMode {
    override val defaultSimpleCases: Map<Class<*>, ArrayWrapper>
        get() = valueSimpleCases + arraySimpleCases
    override val defaultEdgeCases: Map<Class<*>, ArrayWrapper>
        get() = (defaultPrimitiveEdgeCases + mapOf(String::class.java to ArrayWrapper(arrayOf("")))).let {
            it + it.map { (clazz, _) ->
                val emptyArray = ReflectArray.newInstance(clazz, 0)
                emptyArray.javaClass to ArrayWrapper(arrayOf(emptyArray))
            }
        }
    override val defaultGenerators: Map<Class<*>, Gen<*>>
        get() = primitiveGenerators
    override fun findControlClass(clazz: Class<*>, typePool: TypePool): Class<*>?
            = getDefiningKotlinFileClass(clazz, typePool)
}

internal fun getLanguageMode(clazz: Class<*>): LanguageMode {
    return if (clazz.isAnnotationPresent(Metadata::class.java)) KotlinMode else JavaMode
}
