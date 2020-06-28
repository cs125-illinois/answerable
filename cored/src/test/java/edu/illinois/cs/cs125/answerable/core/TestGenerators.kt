package edu.illinois.cs.cs125.answerable.core

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
    "f:it should generate parameters properly" {
        methodNamed("testInt").testParameterGenerator(3, 2, 5)
        methodNamed("testTwoInts").testParameterGenerator(3 * 3, 2 * 2, 5 * 5)
        methodNamed("testIntArray").testParameterGenerator(2, 1, 3)
        methodNamed("testTwoIntArrays").testParameterGenerator(2 * 2, 1 * 1, 3 * 3)
    }
})

private fun methodNamed(name: String) = examples.generatortesting.TestGenerators::class.java.declaredMethods
    .find { it.name == name } ?: error("Couldn't find method $name")

private fun <T> Method.testGenerator(generator: Generator<T>) {
    invoke(null, generator.edge.first().v)
    invoke(null, generator.simple.first().v)
    invoke(null, generator.random(Generator.Complexity()).v)
    invoke(null, generator.random(Generator.Complexity().max()).v)
}

private fun Method.testParameterGenerator(simpleSize: Int, edgeSize: Int, mixedSize: Int) {
    val parameterGenerator = ParameterGenerator(parameters)
    parameterGenerator.simple.also { simple ->
        simple shouldHaveSize simpleSize
        simple.forEach { invoke(null, *it.v) }
    }
    parameterGenerator.edge.also { edge ->
        edge shouldHaveSize edgeSize
        edge.forEach { invoke(null, *it.v) }
    }
    parameterGenerator.mixed.also { mixed ->
        mixed shouldHaveSize mixedSize
        mixed.forEach { invoke(null, *it.v) }
    }
    parameterGenerator.mixed.also { mixed ->
        mixed shouldHaveSize mixedSize
        mixed.forEach { invoke(null, *it.v) }
    }
    Generator.Complexity.ALL.forEach { complexity ->
        invoke(null, *parameterGenerator.random(complexity).v)
    }
}