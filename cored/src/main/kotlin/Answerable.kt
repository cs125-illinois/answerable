@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package edu.illinois.cs.cs125.answerable.core

import java.lang.reflect.Constructor
import java.lang.reflect.Executable
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

    val publicMethods = (solution.declaredMethods.toList() + solution.declaredConstructors.toList()).map {
        it as Executable
    }.filter { !it.isPrivate() }

    val solutionMethods = publicMethods.filterIsInstance<Method>().also {
        check(it.isNotEmpty()) { "Answerable found no methods to test in ${solution.name}" }
    }.toSet()
    val solutionConstructors = publicMethods.filterIsInstance<Constructor<*>>().also {
        check(it.isNotEmpty()) { "Answerable found no available constructors in ${solution.name}" }
    }.toSet()
    val onlyStatic = solutionMethods.all { it.isStatic() }

    fun check(submission: Class<*>, settings: Settings = Settings()): TestPair = TestPair(this, submission, settings)
    fun test(submission: Class<*>, settings: Settings = Settings()) = check(submission, settings).test()
}

fun Set<Method>.cycle() = sequence {
    yield(shuffled().first())
}

class RandomPair(seed: Long = Random().nextLong()) {
    val solution = Random().also { it.setSeed(seed) }
    val submission = Random().also { it.setSeed(seed) }
}

class ClassDesignError(klass: Class<*>, executable: Executable) : Exception(
    "Submission class ${klass.name} didn't provide ${if (executable is Method) {
        "method"
    } else {
        "constructor"
    }} ${executable.fullName()}"
)

class TestPair(
    val answerable: Answerable,
    val submission: Class<*>,
    settings: Answerable.Settings = Answerable.Settings()
) {
    val settings = settings merge answerable.settings

    val submissionConstructors = answerable.solutionConstructors.map { constructor ->
        constructor to (submission.findConstructor(constructor) ?: throw ClassDesignError(submission, constructor))
    }.toMap()
    val submissionMethods = answerable.solutionMethods.map { method ->
        method to (submission.findMethod(method) ?: throw ClassDesignError(submission, method))
    }.toMap()

    fun createReceivers(settings: Answerable.Settings): List<PairRunner> {
        return if (answerable.onlyStatic) {
            listOf(PairRunner(this).also { it.create() })
        } else {
            mutableListOf<PairRunner>().also { receivers ->
                var ready = 0
                for (i in 0 until settings.receiverCount * settings.maxReceiverRetries) {
                    PairRunner(this)
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

    fun List<PairRunner>.cycle() = sequence {
        yield(filter { it.ready }.shuffled().firstOrNull())
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
            receivers.first()?.also {
                it.next()
            } ?: error("Ran out of receivers to test")
        }
    }
}

class PairRunner(val pair: TestPair) {
    var ready = false

    var solution: Any? = null
    var submission: Any? = null

    data class ConstructorResult(val solution: Throwable?, val submission: Throwable?, val method: String? = null) {
        val same = (solution == null && submission == null) || (solution?.equals(submission) ?: false)
    }

    lateinit var constructorResult: ConstructorResult

    @Suppress("TooGenericExceptionCaught")
    fun create(): Boolean {
        constructorResult = if (pair.answerable.onlyStatic) {
            ConstructorResult(null, null)
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
            ConstructorResult(solutionThrew, submissionThrew)
        }.also {
            ready = it.same
        }
        return ready
    }

    val methodIterator = pair.answerable.solutionMethods.cycle()

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
        check(solutionMethod.parameterTypes.isEmpty()) {
            "No support for parameter generation yet"
        }
        val submissionMethod = pair.submissionMethods[solutionMethod] ?: error(
            "Answerable couldn't find a submission method that should exist"
        )

        @SuppressWarnings("TooGenericExceptionCaught")
        val solutionResult = try {
            MethodResult.Result(solutionMethod.invoke(solution), null)
        } catch (e: Throwable) {
            MethodResult.Result(null, e)
        }

        @SuppressWarnings("TooGenericExceptionCaught")
        val submissionResult = try {
            MethodResult.Result(submissionMethod.invoke(submission), null)
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

fun Class<*>.answerable() = Answerable(this)

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