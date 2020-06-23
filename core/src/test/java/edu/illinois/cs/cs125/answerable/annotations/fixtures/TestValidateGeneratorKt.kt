@file:Suppress("unused", "UNUSED_PARAMETER", "FunctionOnlyReturningConstant")

package edu.illinois.cs.cs125.answerable.annotations.fixtures

import edu.illinois.cs.cs125.answerable.annotations.Generator
import edu.illinois.cs.cs125.answerable.api.defaultIntGenerator
import java.util.Random
import kotlin.math.abs

@Generator(name = "correct0")
fun correct0(complexity: Int, random: Random): Int = abs(defaultIntGenerator(complexity, random))

@Generator(name = "broken0")
fun broken0(random: Random): Int = 0

@Generator(name = "broken1")
fun broken1(tvg: TestValidateGeneratorKt, complexity: Int, random: Random): Int = 0

class TestValidateGeneratorKt
