package edu.illinois.cs.cs125.answerable.testing

import edu.illinois.cs.cs125.answerable.annotations.getAllGenerators
import edu.illinois.cs.cs125.answerable.getDefaultAtNext
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.util.Random

internal class ReceiverGenerator private constructor(private val strategy: ReceiverGenStrategy) {
    /**
     * Each of these has a strict requirement for whether or not they are backed by a method,
     * and for the number and types of parameters. These requirements are enforced by annotation
     * validation and by the static factory in the companion object.
     */
    // Initially this was an enum class, but the lack of type safety in constructors was allowing
    // a stupid mistake in GeneratorMapBuilder. Some stuff has changed since, but the sealed class
    // still seems better.
    private sealed class ReceiverGenStrategy(val generator: ((Any?, Int, Random) -> Any?)? = null) {

        class GENERATOR(generator: ((Int, Random) -> Any?))
            : ReceiverGenStrategy({ _, c, r -> generator(c, r) })
        {
            constructor(generator: Method) : this(
                { c: Int, r: Random -> generator.invoke(null, c, r) }
            )
        }

        class NEXT(next: ((Any?, Int, Random) -> Any?))
            : ReceiverGenStrategy(next) {
            constructor(generator: Method) : this(
                { prev: Any?, c: Int, r: Random -> generator.invoke(null, prev, c, r) }
            )
        }

        class DEFAULTCONSTRUCTOR(ctor: Constructor<*>) : ReceiverGenStrategy(
            { _, _, _ -> ctor.newInstance() }
        )

        class NONE : ReceiverGenStrategy()
    }

    fun generate(previous: Any?, iteration: Int, complexity: Int, random: Random): Any? =
        when (strategy) {
            is ReceiverGenStrategy.GENERATOR ->
                strategy.generator?.invoke(null, complexity, random)
            is ReceiverGenStrategy.NEXT ->
                strategy.generator?.invoke(previous, iteration, random)
            /* All arguments in this case are unused, it is either DEFAULTCONSTRUCTOR or NONE */
            else -> strategy.generator?.invoke(null, complexity, random)
        }

    val canMakeInstances: Boolean = strategy !is ReceiverGenStrategy.NONE

    companion object {
        fun of(klass: Class<*>, map: Map<GeneratorRequest, Gen<*>>? = null): ReceiverGenerator {
            val generator: Gen<*>? = map?.get(klass.asGeneratorRequest()) ?:
                klass.getAllGenerators().firstOrNull { it.returnType == klass }?.let { CustomGen(it) }
            val next: Method? = klass.getDefaultAtNext()
            val defaultConstructor: Constructor<*>? = try {
                klass.getConstructor()
            } catch (_: NoSuchMethodException) {
                null
            }

            val strategy = when {
                next != null -> ReceiverGenStrategy.NEXT(next)
                generator != null -> ReceiverGenStrategy.GENERATOR(generator::generate)
                defaultConstructor != null -> ReceiverGenStrategy.DEFAULTCONSTRUCTOR(defaultConstructor)
                else -> ReceiverGenStrategy.NONE()
            }

            return ReceiverGenerator(strategy)
        }
    }
}