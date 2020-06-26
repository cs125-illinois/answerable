@file:Suppress("unused")

package edu.illinois.cs.cs125.answerable.core

import java.lang.reflect.Constructor
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.Random
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor

class Answerable(val solution: Class<*>, val settings: Settings = Settings()) {
    data class Settings(
        val receiverCount: Int = -1,
        val testCount: Int = -1,
        val maxReceiverRetries: Int = -1

    ) {
        companion object {
            val DEFAULTS = Settings(
                receiverCount = 32,
                testCount = 256,
                maxReceiverRetries = 8
            )
        }
    }

    val solutionMethods = solution.declaredMethods.also {
        check(it.isNotEmpty()) { "Answerable found no methods to test in ${solution.name}" }
    }.toSet()
    val solutionConstructors = solution.declaredConstructors.also {
        check(it.isNotEmpty()) { "Answerable found no available constructors in ${solution.name}" }
    }.toSet()
    val onlyStatic = solutionMethods.all { it.isStatic() }

    fun test(submission: Class<*>, settings: Settings = Settings()) {
        Tester(this, submission, settings).test()
    }
}

fun Set<Method>.cycle() = sequence {
    yield(shuffled().first())
}

class RandomPair(seed: Long = Random().nextLong()) {
    val solution = Random().also { it.setSeed(seed) }
    val submission = Random().also { it.setSeed(seed) }
}

class Tester(
    val answerable: Answerable,
    val submission: Class<*>,
    settings: Answerable.Settings = Answerable.Settings()
) {
    val settings = settings merge answerable.settings

    val submissionConstructors: Map<Constructor<*>, Constructor<*>>
    val submissionMethods: Map<Method, Method>

    init {
        submissionConstructors = answerable.solutionConstructors.map { constructor ->
            constructor to (submission.findConstructor(constructor)
                ?: error("Submission didn't provide constructor: $constructor"))
        }.toMap()
        submissionMethods = answerable.solutionMethods.map { method ->
            method to (submission.findMethod(method) ?: error("Submission didn't provide method: $method"))
        }.toMap()
    }

    fun createReceivers(settings: Answerable.Settings): List<Receivers> {
        return if (answerable.onlyStatic) {
            listOf(Receivers(this).also { it.create() })
        } else {
            mutableListOf<Receivers>().also { receivers ->
                var ready = 0
                for (i in 0 until settings.receiverCount * settings.maxReceiverRetries) {
                    Receivers(this)
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
            }.toList()
        }
    }

    fun List<Receivers>.cycle() = sequence {
        yield(filter { it.ready }.shuffled().first())
    }

    fun test() {
        val settings = Answerable.Settings.DEFAULTS merge settings
        val expectedReceivers = if (answerable.onlyStatic) {
            1
        } else {
            settings.receiverCount
        }
        val receivers = createReceivers(settings).also {
            require(it.size == expectedReceivers) {
                "Didn't generate the requested number of receivers: ${it.size} < ${settings.receiverCount}"
            }
        }.cycle()
        repeat(settings.testCount) {
            receivers.first().also {
                it.next()
            }
        }
    }
}

class Receivers(val pair: Tester) {
    var solution: Any? = null
    var submission: Any? = null
    var ready = false
    val methodIterator = pair.answerable.solutionMethods.cycle()

    data class ConstructorResult(val solution: Throwable?, val submission: Throwable?) {
        val same = (solution == null && submission == null) || (solution?.equals(submission) ?: false)
    }

    fun create(): ConstructorResult {
        return if (pair.answerable.onlyStatic) {
            ConstructorResult(null, null).also {
                ready = true
            }
        } else {
            val solutionConstructor = pair.answerable.solutionConstructors.toList().shuffled().take(1).first()
            check(solutionConstructor.parameters.isEmpty()) {
                "No support for parameter generation yet"
            }
            val solutionThrew = try {
                solution = solutionConstructor.newInstance()
                null
            } catch (e: Throwable) {
                e
            }
            val submissionConstructor = pair.submissionConstructors[solutionConstructor] ?: error(
                "Answerable couldn't find a submission constructor that should exist"
            )
            val submissionThrew = try {
                submission = submissionConstructor.newInstance()
                null
            } catch (e: Throwable) {
                e
            }
            ConstructorResult(solutionThrew, submissionThrew).also { ready = it.same }
        }
    }

    data class MethodResult(val solution: Result, val submission: Result) {
        data class Result(val returned: Any?, val threw: Throwable?)

        val sameReturn =
            (solution.returned == null && submission.returned == null) ||
                (solution.returned?.equals(submission.returned) ?: false)
        val sameThrew = (solution.threw == null && submission.threw == null) ||
            (solution.threw?.equals(submission.threw) ?: false)

        val same = sameReturn && sameThrew
    }

    fun next(): Boolean {
        methodIterator.first().also { invoke(it) }
        return ready
    }

    fun invoke(solutionMethod: Method, args: Array<Any?> = arrayOf()): MethodResult {
        check(ready) { "Receiver object is not ready to invoke another method" }

        val submissionMethod = pair.submissionMethods[solutionMethod] ?: error(
            "Answerable couldn't find a submission method that should exist"
        )

        @SuppressWarnings("TooGenericExceptionCaught")
        val solutionResult = try {
            MethodResult.Result(solutionMethod.invoke(solution, *args), null)
        } catch (e: Throwable) {
            MethodResult.Result(null, e)
        }

        @SuppressWarnings("TooGenericExceptionCaught")
        val submissionResult = try {
            MethodResult.Result(submissionMethod.invoke(submission, *args), null)
        } catch (e: Throwable) {
            MethodResult.Result(null, e)
        }

        return MethodResult(solutionResult, submissionResult).also { ready = it.same }
    }
}

fun Class<*>.answerable() = Answerable(this)

fun Method.isStatic() = Modifier.isStatic(modifiers)

fun Class<*>.findMethod(method: Method) = this.declaredMethods.find {
    it?.parameterTypes?.contentEquals(method.parameterTypes) ?: false
}

fun Class<*>.findConstructor(constructor: Constructor<*>) = this.declaredConstructors.find {
    it?.parameterTypes?.contentEquals(constructor.parameterTypes) ?: false
}

inline infix fun <reified T : Any> T.merge(other: T): T {
    val nameToProperty = T::class.declaredMemberProperties.associateBy { it.name }
    val primaryConstructor = T::class.primaryConstructor!!
    val args = primaryConstructor.parameters.associate { parameter ->
        val property = nameToProperty[parameter.name]!!
        val value = if (property.get(other) != -1) {
            property.get(other)
        } else {
            property.get(this)
        }
        parameter to value
    }
    return primaryConstructor.callBy(args)
}