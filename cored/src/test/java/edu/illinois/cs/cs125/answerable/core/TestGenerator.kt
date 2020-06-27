package edu.illinois.cs.cs125.answerable.core

import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.specs.StringSpec
import java.lang.reflect.Method
import java.util.Arrays
import java.util.Random

class TestGenerator : StringSpec({
    "it should determine array enclosed types correctly" {
        IntArray::class.java.getEnclosedType() shouldBe Int::class.java
        Array<IntArray>::class.java.getEnclosedType() shouldBe Int::class.java
        Array<Array<IntArray>>::class.java.getEnclosedType() shouldBe Int::class.java
        Array<Array<Array<String>>>::class.java.getEnclosedType() shouldBe String::class.java
    }
    "it should generate ints properly" {
        val generator = DefaultGenerators[Int::class.java](Random())
        methodNamed("testInt").also { method ->
            method.invoke(null, 0)
            method.invoke(null, generator.edge.first().first)
            method.invoke(null, generator.simple.first().first)
            method.invoke(null, generator.next(1.0).first)
        }
    }
    "it should generate arrays properly" {
        DefaultGenerators[IntArray::class.java](Random()).also { generator ->
            methodNamed("testIntArray").also { method ->
                method.invoke(null, null)
                method.invoke(null, intArrayOf())
                method.invoke(null, intArrayOf(1, 2, 4))
                method.invoke(null, generator.edge.first().first)
                method.invoke(null, generator.simple.first().first)
                method.invoke(null, generator.next(1.0).first)
            }
        }
        DefaultGenerators[Array<IntArray>::class.java](Random()).also { generator ->
            methodNamed("testIntArrayArray").also { method ->
                method.invoke(null, null)
                method.invoke(null, arrayOf(intArrayOf()))
                method.invoke(null, arrayOf(intArrayOf(1, 2, 3), intArrayOf(4, 5, 6)))
                method.invoke(null, generator.edge.first().first)
                method.invoke(null, generator.simple.first().first)
                method.invoke(null, generator.next(1.0).first)
            }
        }
    }
})

private fun methodNamed(name: String) = examples.generatortesting.TestGenerators::class.java.declaredMethods
    .find { it.name == name } ?: error("Couldn't find method $name")