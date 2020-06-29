package edu.illinois.cs.cs125.answerable.core

import edu.illinois.cs.cs125.answerable.core.generators.Defaults
import edu.illinois.cs.cs125.answerable.core.generators.TypeGenerator
import edu.illinois.cs.cs125.answerable.core.generators.TypeParameterGenerator
import edu.illinois.cs.cs125.answerable.core.generators.getArrayType
import edu.illinois.cs.cs125.answerable.core.generators.product
import io.kotlintest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import java.lang.reflect.Method

class TestGenerators : StringSpec({
    "it should determine array enclosed types correctly" {
        IntArray::class.java.getArrayType() shouldBe Int::class.java
        Array<IntArray>::class.java.getArrayType() shouldBe Int::class.java
        Array<Array<IntArray>>::class.java.getArrayType() shouldBe Int::class.java
        Array<Array<Array<String>>>::class.java.getArrayType() shouldBe String::class.java
    }
    "it should generate ints properly" {
        methodNamed("testInt").also { method ->
            val generator = Defaults.create(method.parameterTypes[0])
            method.invoke(null, 0)
            method.testGenerator(generator)
        }
    }
    "it should generate longs properly" {
        methodNamed("testLong").also { method ->
            val generator = Defaults.create(method.parameterTypes[0])
            method.invoke(null, 0)
            method.testGenerator(generator)
        }
    }
    "it should generate Strings properly" {
        methodNamed("testString").also { method ->
            val generator = Defaults.create(method.parameterTypes[0])
            method.invoke(null, null)
            method.invoke(null, "test")
            method.testGenerator(generator)
        }
    }
    "it should generate arrays properly" {
        methodNamed("testIntArray").also { method ->
            val generator = Defaults.create(method.parameterTypes[0])
            method.invoke(null, null)
            method.invoke(null, intArrayOf())
            method.invoke(null, intArrayOf(1, 2, 4))
            method.testGenerator(generator)
        }
        methodNamed("testLongArray").also { method ->
            val generator = Defaults.create(method.parameterTypes[0])
            method.invoke(null, null)
            method.invoke(null, longArrayOf())
            method.invoke(null, longArrayOf(1, 2, 4))
            method.testGenerator(generator)
        }
        methodNamed("testStringArray").also { method ->
            val generator = Defaults.create(method.parameterTypes[0])
            method.invoke(null, null)
            method.invoke(null, arrayOf<String>())
            method.invoke(null, arrayOf("test", "test me"))
            method.testGenerator(generator)
        }
        methodNamed("testIntArrayArray").also { method ->
            val generator = Defaults.create(method.parameterTypes[0])
            method.invoke(null, null)
            method.invoke(null, arrayOf(intArrayOf()))
            method.invoke(null, arrayOf(intArrayOf(1, 2, 3), intArrayOf(4, 5, 6)))
            method.testGenerator(generator)
        }
        methodNamed("testStringArrayArray").also { method ->
            val generator = Defaults.create(method.parameterTypes[0])
            method.invoke(null, null)
            method.invoke(null, arrayOf(arrayOf("")))
            method.invoke(null, arrayOf(arrayOf("test", "me"), arrayOf("again")))
            method.testGenerator(generator)
        }
    }
    "cartesian product should work" {
        listOf(listOf(1, 2), setOf(3, 4)).product().also {
            it shouldHaveSize 4
            it shouldContainExactlyInAnyOrder setOf(listOf(1, 3), listOf(1, 4), listOf(2, 3), listOf(2, 4))
        }
    }
    "it should generate parameters properly" {
        methodNamed("testInt").testParameterGenerator(3, 2)
        methodNamed("testTwoInts").testParameterGenerator(3, 2, 2)
        methodNamed("testIntArray").testParameterGenerator(2, 1)
        methodNamed("testTwoIntArrays").testParameterGenerator(2, 1, 2)
    }
})

private fun methodNamed(name: String) = examples.generatortesting.TestGenerators::class.java.declaredMethods
    .find { it.name == name } ?: error("Couldn't find method $name")

private fun <T> Method.testGenerator(typeGenerator: TypeGenerator<T>) {
    invoke(null, typeGenerator.edge.first().either)
    invoke(null, typeGenerator.simple.first().either)
    invoke(null, typeGenerator.random(TypeGenerator.Complexity()).either)
    invoke(null, typeGenerator.random(TypeGenerator.Complexity().max()).either)
}

private fun Int.pow(exponent: Int) = Math.pow(toDouble(), exponent.toDouble()).toInt()

private fun Method.testParameterGenerator(simpleSize: Int, edgeSize: Int, dimensionality: Int = 1) {
    val parameterGenerator =
        TypeParameterGenerator(parameters)
    parameterGenerator.simple.also { simple ->
        simple shouldHaveSize simpleSize.pow(dimensionality)
        simple.forEach { invoke(null, *it.either) }
    }
    parameterGenerator.edge.also { edge ->
        edge shouldHaveSize edgeSize.pow(dimensionality)
        edge.forEach { invoke(null, *it.either) }
    }
    parameterGenerator.mixed.also { mixed ->
        val mixedSize =
            (simpleSize + edgeSize).pow(dimensionality) - simpleSize.pow(dimensionality) - edgeSize.pow(dimensionality)
        mixed shouldHaveSize mixedSize
        mixed.forEach { invoke(null, *it.either) }
    }
    TypeGenerator.Complexity.ALL.forEach { complexity ->
        invoke(null, *parameterGenerator.random(complexity).either)
    }
}