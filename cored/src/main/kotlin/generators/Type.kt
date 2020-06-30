package edu.illinois.cs.cs125.answerable.core.generators

import java.lang.reflect.Array
import kotlin.math.pow
import kotlin.random.Random

interface TypeGenerator<T> {
    val simple: Set<Value<T>>
    val edge: Set<Value<T?>>
    fun random(complexity: Complexity): Value<T>

    class Complexity(level: Int = MIN) {
        @Suppress("MemberVisibilityCanBePrivate")
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

        fun power(base: Int = 2) = base.toDouble().pow(level.toDouble()).toInt()

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

@Suppress("UNCHECKED_CAST")
class OverrideTypeGenerator(
    klass: Class<*>,
    simple: Set<Any>? = null,
    edge: Set<Any?>? = null,
    random: Random = Random
) : TypeGenerator<Any> {
    val default = Defaults[klass](random)
    private val simpleOverride: Set<TypeGenerator.Value<Any>>? = simple?.values()
    private val edgeOverride: Set<TypeGenerator.Value<Any?>>? = edge?.values()

    override val simple: Set<TypeGenerator.Value<Any>> =
        simpleOverride ?: default.simple as Set<TypeGenerator.Value<Any>>
    override val edge: Set<TypeGenerator.Value<Any?>> =
        edgeOverride ?: default.edge as Set<TypeGenerator.Value<Any?>>

    override fun random(complexity: TypeGenerator.Complexity) = default.random(complexity) as TypeGenerator.Value<Any>
}

sealed class TypeGenerators<T>(internal val random: Random) : TypeGenerator<T>
typealias TypeGeneratorGenerator = (random: Random) -> TypeGenerator<*>

object Defaults {
    private val map = mutableMapOf<Class<*>, TypeGeneratorGenerator>()

    init {
        map[Int::class.java] = IntGenerator.Companion::create
        map[Long::class.java] = LongGenerator.Companion::create
        map[Boolean::class.java] = BooleanGenerator.Companion::create
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
class ArrayGenerator(random: Random, private val klass: Class<*>) : TypeGenerators<Any>(random) {
    private val componentGenerator = Defaults.create(klass, random)
    private val isNested = klass.isArray

    override val simple: Set<TypeGenerator.Value<Any>>
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
    override fun random(complexity: TypeGenerator.Complexity): TypeGenerator.Value<Any> {
        val innerComplexity = if (isNested) {
            TypeGenerator.Complexity(complexity.level - 2)
        } else {
            complexity
        }
        return (Array.newInstance(klass, complexity.power()).also { array ->
            (0 until complexity.power()).forEach { index ->
                Array.set(
                    array,
                    index,
                    componentGenerator.random(innerComplexity).either
                )
            }
        }).value()
    }
}

class IntGenerator(random: Random) : TypeGenerators<Int>(random) {

    override val simple = (-1..1).toSet().values()
    override val edge = setOf<Int?>(Int.MIN_VALUE, Int.MAX_VALUE).values()
    override fun random(complexity: TypeGenerator.Complexity): TypeGenerator.Value<Int> =
        random(complexity, random).value()

    companion object {
        fun random(complexity: TypeGenerator.Complexity, random: Random = Random) = complexity.power().let { bound ->
            random.nextInt(2 * bound) - bound
        }

        fun create(random: Random = Random) =
            IntGenerator(random)
    }
}

class LongGenerator(random: Random) : TypeGenerators<Long>(random) {

    override val simple = (-1L..1L).toSet().values()
    override val edge = setOf<Long?>(Long.MIN_VALUE, Long.MAX_VALUE).values()
    override fun random(complexity: TypeGenerator.Complexity) = random(complexity, random).value()

    companion object {
        @Suppress("MagicNumber")
        fun random(complexity: TypeGenerator.Complexity, random: Random = Random) =
            complexity.power(8).toLong().let { bound ->
                Math.floorMod(random.nextLong(), 2 * bound) - bound
            }

        fun create(random: Random = Random) =
            LongGenerator(random)
    }
}

class BooleanGenerator(random: Random) : TypeGenerators<Boolean>(random) {

    override val simple = setOf(true, false).values()
    override val edge = setOf<Boolean?>().values()
    override fun random(complexity: TypeGenerator.Complexity) = random.nextBoolean().value()

    companion object {
        fun create(random: Random = Random) = BooleanGenerator(random)
    }
}

class StringGenerator(random: Random) : TypeGenerators<String>(random) {

    override val simple = setOf("test", "test string").values()
    override val edge = listOf(null, "").values()
    override fun random(complexity: TypeGenerator.Complexity) = random(complexity, random).value()

    companion object {
        @Suppress("MemberVisibilityCanBePrivate")
        val ALPHANUMERIC_CHARS: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9') + ' '
        fun random(complexity: TypeGenerator.Complexity, random: Random = Random): String {
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