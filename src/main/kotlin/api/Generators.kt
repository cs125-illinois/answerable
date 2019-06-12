@file: JvmName("Generators")
package edu.illinois.cs.cs125.answerable.api

import edu.illinois.cs.cs125.answerable.*
import java.util.*

/**
 * Answerable's default generator for ints.
 */
fun defaultIntGenerator(complexity: Int, random: Random): Int = defaultIntGen(complexity, random)

/**
 * Answerable's default generator for bytes.
 */
fun defaultByteGenerator(complexity: Int, random: Random): Byte = defaultByteGen(complexity, random)

/**
 * Answerable's default generator for shorts.
 */
fun defaultShortGenerator(complexity: Int, random: Random): Short = defaultShortGen(complexity, random)

/**
 * Answerable's default generator for longs.
 */
fun defaultLongGenerator(complexity: Int, random: Random): Long = defaultLongGen(complexity, random)

/**
 * Answerable's default generator for doubles.
 */
fun defaultDoubleGenerator(complexity: Int, random: Random): Double = defaultDoubleGen(complexity, random)

/**
 * Answerable's default generator for floats.
 */
fun defaultFloatGenerator(complexity: Int, random: Random): Float = defaultFloatGen(complexity, random)

/**
 * Answerable's default generator for chars. Is capable of generating some unicode characters that most fonts support.
 */
fun defaultCharGenerator(complexity: Int, random: Random): Char = defaultCharGen(complexity, random)

/**
 * Answerable's secondary generator for chars. Only produces ascii characters \32, ' ', through \254, '~'.
 */
fun defaultAsciiGenerator(complexity: Int, random: Random): Char = defaultAsciiGen(complexity, random)

/**
 * Answerable's default generator for strings. Uses [defaultCharGenerator].
 */
fun defaultStringGenerator(complexity: Int, random: Random): String = DefaultStringGen(defaultCharGen)(complexity, random)

/**
 * Answerable's default generator for ascii-only strings. Uses [defaultAsciiGenerator].
 */
fun defaultAsciiStringGenerator(complexity: Int, random: Random): String = DefaultStringGen(defaultAsciiGen)(complexity, random)