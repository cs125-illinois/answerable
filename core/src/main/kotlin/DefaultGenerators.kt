package edu.illinois.cs.cs125.answerable

import java.util.Random
import kotlin.math.min

internal val defaultIntGen = object : Gen<Int> {
    override fun generate(complexity: Int, random: Random): Int {
        var comp = complexity
        if (complexity > Int.MAX_VALUE / 2) {
            comp = Int.MAX_VALUE / 2
        }
        return random.nextInt(comp * 2 + 1) - comp
    }
}

private const val MAX_FP_DENOMINATOR = (1e10 - 1)

internal val defaultDoubleGen = object : Gen<Double> {
    override fun generate(complexity: Int, random: Random): Double {
        val denom = random.nextDouble() * MAX_FP_DENOMINATOR + 1
        val num = (random.nextDouble() * 2 * complexity * denom) - complexity * denom
        return num / denom
    }
}

internal val defaultFloatGen = object : Gen<Float> {
    override fun generate(complexity: Int, random: Random): Float {
        val denom = random.nextDouble() * MAX_FP_DENOMINATOR + 1
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
        val complexityScaledToLargeValueRange = (complexity.toLong() * complexity.toLong() * 2).coerceAtLeast(0)
        return random.nextLong(complexityScaledToLargeValueRange * 2 + 1) - complexityScaledToLargeValueRange
    }
}

private const val FIRST_PRINTABLE_ASCII = 32
private const val LAST_PRINTABLE_ASCII = 126
private const val NUM_PRINTABLE_ASCII_CHARACTERS = LAST_PRINTABLE_ASCII - FIRST_PRINTABLE_ASCII + 1
private const val FIRST_ASTRAL_PLANE_UNICODE = 0x10000
private const val UNICODE_CHANCE = 0.15
private const val UNICODE_CHANCE_COMPLEXITY_SCALE = 32

internal val defaultCharGen = object : Gen<Char> {
    private fun Char.isPrintableAscii(): Boolean = this.toInt() in FIRST_PRINTABLE_ASCII..LAST_PRINTABLE_ASCII

    private fun Char.isPrint(): Boolean = isPrintableAscii() || Character.UnicodeBlock.of(this) in setOf(
        Character.UnicodeBlock.CYRILLIC,
        Character.UnicodeBlock.CYRILLIC_SUPPLEMENTARY,
        Character.UnicodeBlock.TAMIL,
        Character.UnicodeBlock.CURRENCY_SYMBOLS,
        Character.UnicodeBlock.ARROWS,
        Character.UnicodeBlock.SUPPLEMENTAL_ARROWS_A,
        Character.UnicodeBlock.ETHIOPIC_EXTENDED,
        Character.UnicodeBlock.CJK_RADICALS_SUPPLEMENT,
        Character.UnicodeBlock.KANGXI_RADICALS,
        Character.UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS,
        Character.UnicodeBlock.ENCLOSED_CJK_LETTERS_AND_MONTHS,
        Character.UnicodeBlock.OLD_PERSIAN
    )

    override fun generate(complexity: Int, random: Random): Char {
        val complexityScaledUnicodeChance = complexity * UNICODE_CHANCE / UNICODE_CHANCE_COMPLEXITY_SCALE
        return if (random.nextDouble() < min(complexityScaledUnicodeChance, UNICODE_CHANCE)) {
            var char: Char
            do {
                char = random.nextInt(FIRST_ASTRAL_PLANE_UNICODE).toChar()
            } while (!char.isPrint())
            char
        } else {
            (random.nextInt(NUM_PRINTABLE_ASCII_CHARACTERS) + FIRST_PRINTABLE_ASCII).toChar()
        }
    }
}

internal val defaultAsciiGen = object : Gen<Char> {
    override fun generate(complexity: Int, random: Random): Char {
        return (random.nextInt(NUM_PRINTABLE_ASCII_CHARACTERS) + FIRST_PRINTABLE_ASCII).toChar()
    }
}

internal val defaultBooleanGen = object : Gen<Boolean> {
    override fun generate(complexity: Int, random: Random): Boolean = random.nextBoolean()
}

internal val primitiveGenerators: Map<Class<*>, Gen<*>> = mapOf(
    Int::class.java to defaultIntGen,
    Double::class.java to defaultDoubleGen,
    Float::class.java to defaultFloatGen,
    Byte::class.java to defaultByteGen,
    Short::class.java to defaultShortGen,
    Long::class.java to defaultLongGen,
    Char::class.java to defaultCharGen,
    Boolean::class.java to defaultBooleanGen
)
