package edu.illinois.cs.cs125.answerable.testing

import edu.illinois.cs.cs125.answerable.annotations.getAllGenerators
import edu.illinois.cs.cs125.answerable.answerableParams
import edu.illinois.cs.cs125.answerable.api.BytecodeProvider
import edu.illinois.cs.cs125.answerable.classmanipulation.TypePool
import edu.illinois.cs.cs125.answerable.getDefaultAtNext
import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.util.Random

/**
 * A generator that produces arguments for a single method.
 *
 * Currently restricted to public methods which are not
 * Answerable control methods. This may change in the future.
 */
class MethodArgumentGenerator internal constructor(
    private val generatorMap: GeneratorMapV2,
    val method: Method
) {
    /**
     * A constructor intended for users who don't want Answerable's other testing features,
     * but want to generate random arguments for a method. A bytecode provider is necessary
     * only for methods defined in Kotlin classes with top-level generators.
     */
    constructor(method: Method, bytecodeProvider: BytecodeProvider? = null) : this(
        method.declaringClass.generatorMap(pool = TypePool(bytecodeProvider = bytecodeProvider)),
        method
    )

    private val params: Array<GeneratorRequest> = method.answerableParams
    init {
        if (!generatorMap.canGenerate(params)) {
            // This is a hacky way of getting the generator map to throw the "can't satisfy requests" error
            // without just copying it here.
            generatorMap.generateParams(params, 0, Random())
        }
    }

    /**
     * Generate an array of parameters suitable to be passed to [method].
     */
    fun generateParams(complexity: Int, random: Random) =
        generatorMap.generateParams(params, complexity, random)

    /**
     * Generate a receiver object that can be used for calling [Method.invoke] on [method].
     *
     * [previous] and [iteration] are used if receiver objects are generated with an @Next method.
     * [complexity] and [random] are used if they are generated with an @Generator method.
     */
    fun generateReceiver(previous: Any?, iteration: Int, complexity: Int, random: Random) =
        generatorMap.generateReceiver(previous, iteration, complexity, random)
}

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
