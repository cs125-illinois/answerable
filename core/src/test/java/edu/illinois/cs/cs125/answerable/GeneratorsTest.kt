package edu.illinois.cs.cs125.answerable

import edu.illinois.cs.cs125.answerable.api.defaultIntGenerator
import java.util.Random
import kotlin.math.absoluteValue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GeneratorsTest {

    private lateinit var random: Random

    @BeforeEach
    fun setup() {
        random = Random(0x0403)
    }

    @Test
    fun testZeroComplexitySafe() {
        primitiveGenerators.forEach { (_, gen) -> gen(0, random) }
    }

    @Test
    fun testIntGenRange() {
        val numbers = (1 until 50).map { defaultIntGenerator(it, random) }
        assertTrue(numbers.any { it > 0 })
        assertTrue(numbers.any { it < 0 })
    }

    @Test
    fun testLongGenRange() {
        val numbers = (0 until Int.SIZE_BITS).flatMap { bit ->
            (0 until 10).map { defaultLongGen(1 shl bit, random) }
        }
        assertTrue(numbers.any { it < 0 })
        assertTrue(numbers.any { it > 0 })
        assertTrue(numbers.any { it > Int.MAX_VALUE })
        assertTrue(numbers.any { it.absoluteValue < Int.MAX_VALUE })
        assertTrue(numbers.any { it < Int.MIN_VALUE })
    }

    @Test
    fun testFloatGenFinite() {
        repeat(200) {
            val result = defaultFloatGen(it * it * it, random)
            assertTrue(result.isFinite()) { "Generated non-finite $result on iteration $it" }
        }
    }

    @Test
    fun testFloatGenRange() {
        val numbers = (0 until 400).map { defaultFloatGen(it, random) }
        assertTrue(numbers.any { it.absoluteValue < 200 })
        assertTrue(numbers.any { it > 200 })
        assertTrue(numbers.any { it < -200 })
    }

    @Test
    fun testDoubleGenFinite() {
        repeat(200) {
            val result = defaultDoubleGen(it * it * it, random)
            assertTrue(result.isFinite()) { "Generated non-finite $result on iteration $it" }
        }
    }

    @Test
    fun testDoubleGenRange() {
        val numbers = (0 until 400).map { defaultDoubleGen(it, random) }
        assertTrue(numbers.any { it.absoluteValue < 200 })
        assertTrue(numbers.any { it > 200 })
        assertTrue(numbers.any { it < -200 })
    }

    @Test
    fun testAsciiGenNoUnicode() {
        repeat(200) {
            val char = defaultAsciiGen(it, random)
            assertTrue(char in ' '.rangeTo('~')) { "ASCII generator produced $char" }
        }
    }

    @Test
    fun testUnicodeGenRange() {
        val chars = (0 until 200).map { defaultCharGen(it, random) }
        assertTrue(chars.any { it.toInt() > 127 })
        assertTrue(chars.any { it.toInt() < 127 })
        assertFalse(chars.any { it.toInt() < 32 })
    }
}
