package edu.illinois.cs.cs125.answerable.core.generators

import java.lang.reflect.Array
import kotlin.random.Random

sealed class TypeGenerator<T>(internal val random: Random) {
    abstract val simple: Set<Value<T>>
    abstract val edge: Set<Value<T?>>
    abstract fun random(complexity: Complexity): Value<T?>

    class Complexity(level: Int = MIN) {
        var level = level
            set(value) {
                require(value in MIN..MAX) { "Invalid complexity value: $value" }
                field = value
            }

        fun next(): Complexity {
            if (level < MAX) {
                level++
            }
            return this
        }

        fun prev(): Complexity {
            if (level > MIN) {
                level--
            }
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
            val ALL = (MIN..MAX).map { Complexity(it) }
        }
    }

    data class Value<T>(val solution: T, val submission: T) {
        val either = solution
    }
}

typealias TypeGeneratorGenerator = (random: Random) -> TypeGenerator<*>

object Defaults {
    private val map = mutableMapOf<Class<*>, TypeGeneratorGenerator>()

    init {
        map[Int::class.java] = IntGenerator.Companion::create
        map[Long::class.java] = LongGenerator.Companion::create
        map[String::class.java] = StringGenerator.Companion::create
    }

    operator fun get(klass: Class<*>): TypeGeneratorGenerator {
        map[klass]?.also { return it }
        if (klass.isArray && map.containsKey(klass.getArrayType())) {
            return { random ->
                ArrayGenerator(
                    random,
                    klass.componentType
                )
            }
        }
        error("Cannot find generator for class ${klass.name}")
    }

    fun create(klass: Class<*>, random: Random = Random): TypeGenerator<*> = get(klass)(random)
}

@Suppress("UNCHECKED_CAST")
class ArrayGenerator(random: Random, val klass: Class<*>) : TypeGenerator<Any>(random) {
    private val componentGenerator = Defaults.create(klass, random)

    override val simple: Set<Value<Any>>
        get() {
            val simpleCases = componentGenerator.simple.map { it.either }
            return setOf(
                Array.newInstance(klass, 0),
                Array.newInstance(klass, simpleCases.size).also { array ->
                    simpleCases.forEachIndexed { index, value ->
                        Array.set(array, index, value)
                    }
                }
            ).values()
        }
    override val edge = setOf<Any?>(null).values()
    override fun random(complexity: Complexity): Value<Any?> {
        return (Array.newInstance(klass, complexity.power() + 2).also { array ->
            (0 until complexity.power() + 2).forEach { index ->
                Array.set(
                    array,
                    index,
                    componentGenerator.random(complexity).either
                )
            }
        }).value()
    }
}

class IntGenerator(random: Random) : TypeGenerator<Int>(random) {

    override val simple = (-1..1).toSet().values()
    override val edge = setOf<Int?>(Int.MIN_VALUE, Int.MAX_VALUE).values()
    override fun random(complexity: Complexity): Value<Int?> = random(complexity, random).value()

    companion object {
        fun random(complexity: Complexity, random: Random = Random) = complexity.power().let { bound ->
            random.nextInt(2 * bound) - bound
        }

        fun create(random: Random = Random) =
            IntGenerator(random)
    }
}

class LongGenerator(random: Random) : TypeGenerator<Long>(random) {

    override val simple = (-1L..1L).toSet().values()
    override val edge = setOf<Long?>(Long.MIN_VALUE, Long.MAX_VALUE).values()
    override fun random(complexity: Complexity): Value<Long?> = random(complexity, random).value()

    companion object {
        @Suppress("MagicNumber")
        fun random(complexity: Complexity, random: Random = Random) = complexity.power(8).toLong().let { bound ->
            Math.floorMod(random.nextLong(), 2 * bound) - bound
        }

        fun create(random: Random = Random) =
            LongGenerator(random)
    }
}

class StringGenerator(random: Random) : TypeGenerator<String>(random) {

    override val simple = setOf("test", "test string").values()
    override val edge = listOf(null, "").values()
    override fun random(complexity: Complexity): Value<String?> = random(complexity, random).value()

    companion object {
        @Suppress("MemberVisibilityCanBePrivate")
        val ALPHANUMERIC_CHARS: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9') + ' '
        fun random(complexity: Complexity, random: Random = Random): String {
            return (1..complexity.power())
                .map { random.nextInt(ALPHANUMERIC_CHARS.size) }
                .map(ALPHANUMERIC_CHARS::get)
                .joinToString("")
        }

        fun create(random: Random = Random) =
            StringGenerator(random)
    }
}

fun <T> Collection<T>.values() = toSet().also {
    check(size == it.size) { "Collection of values was not distinct" }
}.map { TypeGenerator.Value(it, it) }.toSet()

fun <T> T.value() = TypeGenerator.Value(this, this)
fun <T> Class<T>.getArrayType(start: Boolean = true): Class<*> {
    check(!start || isArray) { "Must be called on an array type" }
    return if (!isArray) {
        this
    } else {
        this.componentType.getArrayType(false)
    }
}