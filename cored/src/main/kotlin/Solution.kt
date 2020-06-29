@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package edu.illinois.cs.cs125.answerable.core

import edu.illinois.cs.cs125.answerable.core.generators.EmptyParameters
import edu.illinois.cs.cs125.answerable.core.generators.MethodGenerators
import edu.illinois.cs.cs125.answerable.core.generators.ParameterGeneratorFactory
import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.random.Random
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor

class Solution(val solution: Class<*>, val settings: Settings = Settings()) {
    data class Settings(
        val receiverCount: Int = -1,
        val testCount: Int = -1,
        val maxReceiverRetries: Int = -1,
        val seed: Int = -1,
        val simpleCount: Int = -1,
        val edgeCount: Int = -1,
        val mixedCount: Int = -1,
        val fixedCount: Int = -1
    ) {
        companion object {
            val DEFAULTS = Settings(
                receiverCount = 32,
                testCount = 256,
                maxReceiverRetries = 8,
                simpleCount = Int.MAX_VALUE,
                edgeCount = Int.MAX_VALUE,
                mixedCount = Int.MAX_VALUE,
                fixedCount = Int.MAX_VALUE
            )
        }
    }

    val publicMethods = (solution.declaredMethods.toList() + solution.declaredConstructors.toList()).map {
        it as Executable
    }.filter { !it.isPrivate() }
    val parameterGeneratorFactory: ParameterGeneratorFactory =
        ParameterGeneratorFactory(publicMethods)

    val solutionMethods = publicMethods.filterIsInstance<Method>().also {
        check(it.isNotEmpty()) { "Answerable found no methods to test in ${solution.name}" }
    }.toSet()
    val solutionConstructors = publicMethods.filterIsInstance<Constructor<*>>().also {
        check(it.isNotEmpty()) { "Answerable found no available constructors in ${solution.name}" }
    }.toSet()

    val onlyStatic = solutionMethods.all { it.isStatic() }

    fun submission(submission: Class<*>) = Pair(this, submission)
}

fun Set<Method>.cycle() = sequence {
    yield(shuffled().first())
}

class RandomPair(seed: Long = Random.nextLong()) {
    val solution = java.util.Random().also { it.setSeed(seed) }
    val submission = java.util.Random().also { it.setSeed(seed) }
    val synced: Boolean
        get() = solution.nextLong() == submission.nextLong()
}

class ClassDesignError(klass: Class<*>, executable: Executable) : Exception(
    "Submission class ${klass.name} didn't provide ${if (executable is Method) {
        "method"
    } else {
        "constructor"
    }} ${executable.fullName()}"
)

class Pair(val solution: Solution, val submission: Class<*>) {
    val submissionConstructors = solution.solutionConstructors.map { constructor ->
        constructor to (submission.findConstructor(constructor) ?: throw ClassDesignError(submission, constructor))
    }.toMap()
    val submissionMethods = solution.solutionMethods.map { method ->
        method to (submission.findMethod(method) ?: throw ClassDesignError(submission, method))
    }.toMap()

    fun createReceivers(
        settings: Solution.Settings, methodParameterGenerators: MethodGenerators
    ): List<PairRunner> {
        return if (solution.onlyStatic) {
            listOf(PairRunner(0, this, methodParameterGenerators).also { it.create() })
        } else {
            mutableListOf<PairRunner>().also { receivers ->
                var ready = 0
                for (i in 0 until settings.receiverCount * settings.maxReceiverRetries) {
                    PairRunner(receivers.size, this, methodParameterGenerators)
                        .also { it.create() }
                        .also {
                            if (it.ready) {
                                ready++
                            }
                        }.also {
                            receivers.add(it)
                        }
                    if (ready == settings.receiverCount) {
                        break
                    }
                }
                check(receivers.size == settings.receiverCount) {
                    "Couldn't create the requested number of receivers"
                }
            }.toList()
        }
    }

    fun List<PairRunner>.cycle(random: Random = Random) = sequence {
        yield(filter { it.ready }.shuffled(random).firstOrNull())
    }

    fun test(settings: Solution.Settings = Solution.Settings()) {
        val testSettings = Solution.Settings.DEFAULTS merge settings

        val random = if (settings.seed == -1) {
            Random(Random.nextLong())
        } else {
            Random(settings.seed.toLong())
        }

        val methodGenerators = solution.parameterGeneratorFactory.get(random, testSettings)
        val receivers = createReceivers(testSettings, methodGenerators).cycle(random)
        for (i in 0 until testSettings.testCount) {
            receivers.first()?.next() ?: error("Ran out of receivers to test")
        }
    }
}

class PairRunner(val index: Int, val pair: Pair, val methodGenerators: MethodGenerators) {
    var ready = false

    var solution: Any? = null
    var submission: Any? = null

    data class ConstructorResult(val solution: Throwable?, val submission: Throwable?, val method: String? = null) {
        val same = (solution == null && submission == null) || (solution?.equals(submission) ?: false)
    }

    lateinit var constructorResult: ConstructorResult

    @Suppress("TooGenericExceptionCaught")
    fun create(): Boolean {
        constructorResult = if (pair.solution.onlyStatic) {
            ConstructorResult(null, null)
        } else {
            val solutionConstructor = pair.solution.solutionConstructors.toList().shuffled().take(1).first()
            val parameters = if (solutionConstructor.parameters.isEmpty()) {
                EmptyParameters
            } else {
                methodGenerators[solutionConstructor]!!.generate()
            }
            val solutionThrew = try {
                solution = solutionConstructor.newInstance(*parameters.solution)
                null
            } catch (e: Throwable) {
                e
            }
            val submissionConstructor = pair.submissionConstructors[solutionConstructor] ?: error(
                "Answerable couldn't find a submission constructor that should exist"
            )
            val submissionThrew = try {
                submission = submissionConstructor.newInstance(*parameters.submission)
                null
            } catch (e: Throwable) {
                e
            }
            ConstructorResult(solutionThrew, submissionThrew)
        }.also {
            ready = it.same
        }
        return ready
    }

    val methodIterator = pair.solution.solutionMethods.cycle()

    data class MethodResult(val solution: Result, val submission: Result) {
        data class Result(val returned: Any?, val threw: Throwable?)

        val sameReturn =
            (solution.returned == null && submission.returned == null) ||
                (solution.returned?.equals(submission.returned) ?: false)
        val sameThrew = (solution.threw == null && submission.threw == null) ||
            (solution.threw?.equals(submission.threw) ?: false)

        val same = sameReturn && sameThrew
    }

    val methodResults: MutableList<MethodResult> = mutableListOf()

    fun next(): Boolean {
        check(ready) { "Receiver object is not ready to invoke another method" }

        val solutionMethod = methodIterator.first()
        val parameters = if (solutionMethod.parameters.isEmpty()) {
            EmptyParameters
        } else {
            methodGenerators[solutionMethod]!!.generate()
        }
        val submissionMethod = pair.submissionMethods[solutionMethod] ?: error(
            "Answerable couldn't find a submission method that should exist"
        )

        @SuppressWarnings("TooGenericExceptionCaught")
        val solutionResult = try {
            MethodResult.Result(solutionMethod.invoke(solution, *parameters.solution), null)
        } catch (e: Throwable) {
            MethodResult.Result(null, e)
        }

        @SuppressWarnings("TooGenericExceptionCaught")
        val submissionResult = try {
            MethodResult.Result(submissionMethod.invoke(submission, *parameters.submission), null)
        } catch (e: Throwable) {
            MethodResult.Result(null, e)
        }

        MethodResult(solutionResult, submissionResult).also {
            methodResults.add(it)
            ready = it.same
            return it.same
        }
    }
}

fun solution(klass: Class<*>) = Solution(klass)

fun Executable.isStatic() = Modifier.isStatic(modifiers)
fun Executable.isPrivate() = Modifier.isPrivate(modifiers)
fun Executable.fullName() = "$name(${parameters.joinToString(", ") { it.type.name }})"

fun Class<*>.findMethod(method: Method) = this.declaredMethods.find {
    it?.parameterTypes?.contentEquals(method.parameterTypes) ?: false
}

fun Class<*>.findConstructor(constructor: Constructor<*>) = this.declaredConstructors.find {
    it?.parameterTypes?.contentEquals(constructor.parameterTypes) ?: false
}

inline infix fun <reified T : Any> T.merge(other: T): T {
    val nameToProperty = T::class.declaredMemberProperties.associateBy { it.name }
    val primaryConstructor = T::class.primaryConstructor!!
    val args = primaryConstructor.parameters.associateWith { parameter ->
        val property = nameToProperty[parameter.name]!!
        val value = if (property.get(other) != -1) {
            property.get(other)
        } else {
            property.get(this)
        }
        value
    }
    return primaryConstructor.callBy(args)
}