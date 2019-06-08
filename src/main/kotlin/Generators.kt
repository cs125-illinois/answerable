@file: JvmName("Generators")
package edu.illinois.cs.cs125.answerable

import java.util.*

fun defaultIntGenerator(complexity: Int, random: Random): Int = defaultIntGen(complexity, random)
fun defaultByteGenerator(complexity: Int, random: Random): Byte = defaultByteGen(complexity, random)
fun defaultShortGenerator(complexity: Int, random: Random): Short = defaultShortGen(complexity, random)
fun defaultLongGenerator(complexity: Int, random: Random): Long = defaultLongGen(complexity, random)
fun defaultDoubleGenerator(complexity: Int, random: Random): Double = defaultDoubleGen(complexity, random)
fun defaultFloatGenerator(complexity: Int, random: Random): Float = defaultFloatGen(complexity, random)
fun defaultCharGenerator(complexity: Int, random: Random): Char = defaultCharGen(complexity, random)
fun defaultAsciiGenerator(complexity: Int, random: Random): Char = defaultAsciiGen(complexity, random)
fun defaultStringGenerator(complexity: Int, random: Random): String = DefaultStringGen(defaultCharGen)(complexity, random)