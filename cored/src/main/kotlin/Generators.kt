package edu.illinois.cs.cs125.answerable.core

import java.util.Random
import java.lang.reflect.Array

sealed class Generator<T>(internal val random: Random) {
    abstract val simple: List<Value<T>>
    abstract val edge: List<Value<T?>>
    abstract fun random(complexity: Complexity): Value<T?>

    class Complexity(level: Int = MIN) {
        var level = level
            set(value) {
                if (value in MIN..MAX) {
                    field = value
                }
            }

        fun next(): Complexity {
            level++
            return this
        }

        fun max(): Complexity {
            level = MAX
            return this
        }

        fun power(base: Int = 2) = Math.pow(base.toDouble(), level.toDouble()).toInt()

        companion object {
            const val MIN = 1
            const val MAX = 8
        }
    }

    data class Value<T>(val solution: T, val submission: T) {
        val v = solution
    }
}

object Defaults {
    private val map = mutableMapOf<Class<*>, (random: Random) -> Generator<*>>()

    init {
        map[Int::class.java] = IntGenerator.Companion::create
        map[Long::class.java] = LongGenerator.Companion::create
        map[String::class.java] = StringGenerator.Companion::create
    }

    fun get(klass: Class<*>, random: Random = Random()): Generator<*> {
        map[klass]?.also { return it(random) }
        if (klass.isArray && map.containsKey(klass.getArrayType())) {
            return ArrayGenerator(random, klass.componentType)
        }
        error("Cannot find generator for class ${klass.name}")
    }
}

@Suppress("UNCHECKED_CAST")
class ArrayGenerator(random: Random, val klass: Class<*>) : Generator<Any>(random) {
    private val componentGenerator = Defaults.get(klass, random)
    override val simple: List<Value<Any>>
        get() {
            val simpleCases = componentGenerator.simple.map { it.v }
            return listOf(
                Array.newInstance(klass, 0),
                Array.newInstance(klass, simpleCases.size).also { array ->
                    simpleCases.forEachIndexed { index, value ->
                        Array.set(array, index, value)
                    }
                }
            ).values()
        }
    override val edge: List<Value<Any?>>
        get() = listOf(null).values()

    override fun random(complexity: Complexity): Value<Any?> {
        return (Array.newInstance(klass, complexity.power() + 2).also { array ->
            (0 until complexity.power() + 2).forEach { index ->
                Array.set(array, index, componentGenerator.random(complexity).v)
            }
        }).value()
    }
}

class IntGenerator(random: Random) : Generator<Int>(random) {

    override val simple: List<Value<Int>> = (-1..1).toList().values()
    override val edge: List<Value<Int?>> = listOf(Int.MIN_VALUE, Int.MAX_VALUE).values()
    override fun random(complexity: Complexity): Value<Int?> = random(complexity, random).value()

    companion object {
        fun random(complexity: Complexity, random: Random = Random()) = complexity.power().let { bound ->
            random.nextInt(2 * bound) - bound
        }
        fun create(random: Random = Random()) = IntGenerator(random)
    }
}

class LongGenerator(random: Random) : Generator<Long>(random) {

    override val simple: List<Value<Long>> = (-1L..1L).toList().values()
    override val edge: List<Value<Long?>> = listOf(Long.MIN_VALUE, Long.MAX_VALUE).values()
    override fun random(complexity: Complexity): Value<Long?> = random(complexity, random).value()

    companion object {
        fun random(complexity: Complexity, random: Random = Random()) = complexity.power(8).toLong().let { bound ->
            Math.floorMod(random.nextLong(), 2 * bound) - bound
        }
        fun create(random: Random = Random()) = LongGenerator(random)
    }
}

class StringGenerator(random: Random) : Generator<String>(random) {

    override val simple: List<Value<String>> = listOf("test", "test string").values()
    override val edge: List<Value<String?>> = listOf(null, "").values()
    override fun random(complexity: Complexity): Value<String?> = random(complexity, random).value()

    companion object {
        val ALPHANUMERIC_CHARS: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9') + ' '
        fun random(complexity: Complexity, random: Random = Random()): String {
            return (1..complexity.power())
                .map { random.nextInt(ALPHANUMERIC_CHARS.size) }
                .map(ALPHANUMERIC_CHARS::get)
                .joinToString("")
        }
        fun create(random: Random = Random()) = StringGenerator(random)
    }
}

fun <T> List<T>.values() = map { Generator.Value(it, it) }
fun <T> T.value() = Generator.Value(this, this)

fun <T> Class<T>.getArrayType(start: Boolean = true): Class<*> {
    check(!start || isArray) { "Must be called on an array type" }
    return if (!isArray) {
        this
    } else {
        this.componentType.getArrayType(false)
    }
}
