package edu.illinois.cs.cs125.answerable.core.generators

import edu.illinois.cs.cs125.answerable.core.Solution
import java.lang.reflect.Executable
import java.lang.reflect.Parameter
import java.lang.reflect.Type
import kotlin.random.Random

class ParameterGeneratorFactory(executables: List<Executable>) {
    val typeGenerators: MutableMap<Type, TypeGeneratorGenerator> = mutableMapOf()
    val parameterGenerators: Map<Executable, ParametersGeneratorGenerator> = executables
        .map { executable ->
            TypeParameterGenerator(
                executable.parameters,
                typeGenerators
            )
            Pair<Executable, ParametersGeneratorGenerator>(
                executable,
                { random ->
                    TypeParameterGenerator(
                        executable.parameters,
                        typeGenerators,
                        random
                    )
                })
        }
        .toMap()

    fun get(random: Random = Random, settings: Solution.Settings) = parameterGenerators
        .map { (executable, generatorGenerator) ->
            executable to ConfiguredParametersGenerator(generatorGenerator, settings, random)
        }
        .toMap()
        .let {
            MethodGenerators(it)
        }
}

class MethodGenerators(
    private val map: Map<Executable, ConfiguredParametersGenerator>
) : Map<Executable, ConfiguredParametersGenerator> by map

class ConfiguredParametersGenerator(
    parametersGenerator: ParametersGeneratorGenerator,
    val settings: Solution.Settings,
    val random: Random = Random
) {
    private val generator = parametersGenerator(random)

    fun List<ParametersGenerator.Value>.trim(count: Int) = if (this.size <= count) {
        this
    } else {
        this.shuffled(random).take(count)
    }

    val fixed: List<ParametersGenerator.Value> = generator.let {
        it.simple.trim(settings.simpleCount) + it.edge.trim(settings.simpleCount) + it.mixed.trim(settings.mixedCount)
    }.trim(settings.fixedCount)

    private var index = 0
    private var bound: TypeGenerator.Complexity? = null
    private val complexity = TypeGenerator.Complexity()

    fun generate(): ParametersGenerator.Value {
        return if (index in fixed.indices) {
            fixed[index]
        } else {
            generator.random(bound ?: complexity)
        }.also {
            index++
        }
    }

    fun next() {
        complexity.next()
    }

    fun prev() {
        if (bound == null) {
            bound = complexity
        } else {
            bound!!.prev()
        }
    }
}

typealias ParametersGeneratorGenerator = (random: Random) -> ParametersGenerator

interface ParametersGenerator {
    enum class Type { EMPTY, SIMPLE, EDGE, MIXED, RANDOM }

    val simple: List<Value>
    val edge: List<Value>
    val mixed: List<Value>
    fun random(complexity: TypeGenerator.Complexity): Value

    data class Value(
        val solution: Array<Any?>,
        val submission: Array<Any?>,
        val type: Type,
        val complexity: TypeGenerator.Complexity = TypeGenerator.Complexity(0)
    ) {
        val either = solution
        override fun equals(other: Any?): Boolean {
            return when {
                this === other -> true
                other is Value -> either.contentDeepEquals(other.either)
                else -> false
            }
        }

        override fun hashCode(): Int {
            return either.contentHashCode()
        }
    }
}

val EmptyParameters = ParametersGenerator.Value(arrayOf(), arrayOf(), ParametersGenerator.Type.EMPTY)

class TypeParameterGenerator(
    parameters: Array<out Parameter>,
    generators: Map<Type, TypeGeneratorGenerator> = mapOf(),
    val random: Random = Random
) : ParametersGenerator {
    private val parameterGenerators = parameters.map {
        val type = it.parameterizedType
        if (type in generators) {
            generators[type]
        } else {
            require(type is Class<*>) { "No default generators are registered for non-class types" }
            Defaults[type]
        }?.invoke(random) ?: error(
            "Couldn't find generator for parameter ${it.name} with type ${it.parameterizedType.typeName}"
        )
    }

    private fun List<Set<TypeGenerator.Value<*>>>.combine(type: ParametersGenerator.Type) = product().map { list ->
        list.map {
            check(it is TypeGenerator.Value<*>) { "Didn't find the right type in our parameter list" }
            Pair(it.solution, it.submission)
        }.unzip().let { (solution, submission) ->
            ParametersGenerator.Value(
                solution.toTypedArray(),
                submission.toTypedArray(),
                type
            )
        }
    }

    override val simple by lazy {
        parameterGenerators.map { it.simple }.combine(ParametersGenerator.Type.SIMPLE)
    }
    override val edge by lazy {
        parameterGenerators.map {
            if (it.edge.isNotEmpty()) {
                it.edge
            } else {
                it.simple
            }
        }.combine(ParametersGenerator.Type.EDGE)
    }
    override val mixed by lazy {
        parameterGenerators.map { it.simple + it.edge }.combine(ParametersGenerator.Type.MIXED).filter {
            it !in simple && it !in edge
        }
    }

    override fun random(complexity: TypeGenerator.Complexity): ParametersGenerator.Value {
        return parameterGenerators.map { it.random(complexity) }.map {
            Pair(it.solution, it.submission)
        }.unzip().let { (solution, submission) ->
            ParametersGenerator.Value(
                solution.toTypedArray(),
                submission.toTypedArray(),
                ParametersGenerator.Type.RANDOM,
                complexity
            )
        }
    }
}

fun List<*>.product() = fold(listOf(listOf<Any?>())) { acc, set ->
    require(set is Collection<*>) { "Error computing product" }
    acc.flatMap { list -> set.map { element -> list + element } }
}.toSet()
