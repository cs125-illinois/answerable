package edu.illinois.cs.cs125.answerable.testing

import edu.illinois.cs.cs125.answerable.api.defaultIntGenerator
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Random
import kotlin.math.absoluteValue

class GeneratorTests {

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
            (0 until 10).map {
                defaultLongGen(
                    1 shl bit,
                    random
                )
            }
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
            val result =
                defaultFloatGen(it * it * it, random)
            assertTrue(result.isFinite()) { "Generated non-finite $result on iteration $it" }
        }
    }

    @Test
    fun testFloatGenRange() {
        val numbers = (0 until 400).map {
            defaultFloatGen(
                it,
                random
            )
        }
        assertTrue(numbers.any { it.absoluteValue < 200 })
        assertTrue(numbers.any { it > 200 })
        assertTrue(numbers.any { it < -200 })
    }

    @Test
    fun testDoubleGenFinite() {
        repeat(200) {
            val result =
                defaultDoubleGen(it * it * it, random)
            assertTrue(result.isFinite()) { "Generated non-finite $result on iteration $it" }
        }
    }

    @Test
    fun testDoubleGenRange() {
        val numbers = (0 until 400).map {
            defaultDoubleGen(
                it,
                random
            )
        }
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
        val chars = (0 until 200).map {
            defaultCharGen(
                it,
                random
            )
        }
        assertTrue(chars.any { it.toInt() > 127 })
        assertTrue(chars.any { it.toInt() < 127 })
        assertFalse(chars.any { it.toInt() < 32 })
    }

    @Test
    fun testBooleanGenRange() {
        val answers = (0 until 50).map {
            defaultBooleanGen(
                it,
                random
            )
        }
        assertTrue(answers.any { it })
        assertTrue(answers.any { !it })
    }

    @Test
    fun testIntGenIntensifies() {
        val averages = (0 until 20).map {
            (0 until 100).map { _ ->
                defaultIntGen(
                    it * it * it,
                    random
                ).absoluteValue
            }.average()
        }
        averages.foldIndexed(-1.0) { iter, last, value ->
            assertTrue(value > last) { "Did not intensify on iteration $iter: $value <= $last" }
            value
        }
    }

    @Test
    fun testShortGenIntensifies() {
        val averages = (0 until 15).map {
            (0 until 100).map { _ ->
                defaultShortGen(it * it * it, random).toInt().absoluteValue
            }.average()
        }
        averages.foldIndexed(-1.0) { iter, last, value ->
            assertTrue(value > last) { "Did not intensify on iteration $iter: $value <= $last" }
            value
        }
    }

    @Test
    fun testLongGenIntensifies() {
        val averages = (0 until 50).map {
            (0 until 200).map { _ ->
                defaultLongGen(
                    it * it * it,
                    random
                ).absoluteValue
            }.average()
        }
        averages.foldIndexed(-1.0) { iter, last, value ->
            assertTrue(value > last) { "Did not intensify on iteration $iter: $value <= $last" }
            value
        }
    }

    @Test
    fun testFloatGenIntensifies() {
        val averages = (0 until 50).map {
            (0 until 1000).map { _ ->
                defaultFloatGen(
                    it * it * it,
                    random
                ).absoluteValue
            }.average()
        }
        averages.foldIndexed(-1.0) { iter, last, value ->
            assertTrue(value > last) { "Did not intensify on iteration $iter: $value <= $last" }
            value
        }
    }

    @Test
    fun testDoubleGenIntensifies() {
        val averages = (0 until 80).map {
            (0 until 1000).map { _ ->
                defaultDoubleGen(
                    it * it * it * it,
                    random
                ).absoluteValue
            }.average()
        }
        averages.foldIndexed(-1.0) { iter, last, value ->
            assertTrue(value > last) { "Did not intensify on iteration $iter: $value <= $last" }
            value
        }
    }

    @Test
    fun testCharGenIntensifies() {
        val unicodeCounts = (0 until 10).map {
            (0 until 1000).filter { _ ->
                defaultCharGen(
                    it * 3,
                    random
                ).toInt() > 127
            }.count()
        }
        unicodeCounts.foldIndexed(-1) { iter, last, value ->
            assertTrue(value > last) { "Did not intensify on iteration $iter: $value <= $last" }
            value
        }
    }

    @Test
    fun testStringGenIntensifies() {
        val generator = DefaultStringGen(defaultAsciiGen)
        val maxLengths = (0 until 50).map {
            (0 until 100).map { _ -> generator(it * it, random).length }.max()!!
        }
        maxLengths.foldIndexed(-1) { iter, last, value ->
            assertTrue(value > last) { "Did not intensify on iteration $iter: $value <= $last" }
            value
        }
    }
}
