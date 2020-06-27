package edu.illinois.cs.cs125.answerable.core

import java.util.Random
import kotlin.math.roundToInt
import java.lang.reflect.Array

sealed class Generator<T>(internal val random: Random) {
    abstract val simple: List<Pair<T, T>>
    abstract val edge: List<Pair<T?, T?>>
    abstract fun next(percent: Double): Pair<T?, T?>
}

object DefaultGenerators {
    private val map = mutableMapOf<Class<*>, (random: Random) -> Generator<*>>()

    init {
        map[Int::class.java] = { random -> IntGenerator(random) }
    }

    @Suppress("ReturnCount")
    operator fun get(key: Class<*>): ((random: Random) -> Generator<*>) {
        map[key]?.also {
            return it
        }
        if (key.isArray && map.containsKey(key.getEnclosedType())) {
            return { random -> ArrayGenerator(random, key.componentType) }
        }
        error("Cannot find generator for type ${key.name}")
    }
}

@Suppress("UNCHECKED_CAST")
class ArrayGenerator(
    random: Random,
    val klass: Class<*>,
    private val maxLength: Int = MAX_LENGTH
) : Generator<Any>(random) {
    private val componentGenerator = DefaultGenerators[klass](random)
    override val simple: List<Pair<Any, Any>>
        get() {
            val simpleCases = componentGenerator.simple.map { it.first }
            return listOf(
                Array.newInstance(klass, 0),
                Array.newInstance(klass, simpleCases.size).also { array ->
                    simpleCases.forEachIndexed { index, value ->
                        Array.set(array, index, value)
                    }
                }
            ).pairList()
        }
    override val edge: List<Pair<Any?, Any?>>
        get() = listOf(null).pairList()

    override fun next(percent: Double): Pair<Any?, Any?> {
        val length = (maxLength * percent).roundToInt()
        return (Array.newInstance(klass, length).also { array ->
            (0 until length).forEach { index ->
                Array.set(array, index, componentGenerator.next(percent).first)
            }
        }).pairElement()
    }

    companion object {
        const val MAX_LENGTH = 1024
    }
}

class IntGenerator(
    random: Random, private val min: Int = MIN, private val max: Int = MAX
) : Generator<Int>(random) {
    private fun intBetween(min: Int, max: Int) = random.nextInt(max - min) + min

    override val simple: List<Pair<Int, Int>>
        get() = (-1..1).toList().pairList()
    override val edge: List<Pair<Int, Int>>
        get() = listOf(Int.MIN_VALUE, Int.MAX_VALUE).pairList()

    override fun next(percent: Double): Pair<Int, Int> = ((intBetween(min, max) * percent).roundToInt()).pairElement()

    companion object {
        const val MIN = -1024 * 1024
        const val MAX = 1024 * 1024
    }
}

fun <T> List<T>.pairList() = map { Pair(it, it) }
fun <T> T.pairElement() = Pair(this, this)

fun Class<*>.getEnclosedType(): Class<*> {
    return if (!isArray) {
        this
    } else {
        this.componentType.getEnclosedType()
    }
}