@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package edu.illinois.cs.cs125.answerable.core

import edu.illinois.cs.cs125.answerable.core.generators.EmptyParameters
import edu.illinois.cs.cs125.answerable.core.generators.MethodGenerators
import edu.illinois.cs.cs125.answerable.core.generators.ParameterGeneratorFactory
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.concurrent.locks.ReentrantLock
import kotlin.random.Random
import kotlin.concurrent.withLock
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor

class Solution(
    val solution: Class<*>,
    val settings: Settings = Settings(),
    val captureOutput: CaptureOutput = ::defaultCaptureOutput
) {
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
    val parameterGeneratorFactory: ParameterGeneratorFactory = ParameterGeneratorFactory(publicMethods, solution)

    val solutionMethods = publicMethods.filterIsInstance<Method>().also {
        check(it.isNotEmpty()) { "Answerable found no methods to test in ${solution.name}" }
    }.toSet()
    val solutionConstructors = publicMethods.filterIsInstance<Constructor<*>>().also {
        check(it.isNotEmpty()) { "Answerable found no available constructors in ${solution.name}" }
    }.toSet()

    val onlyStatic = solutionMethods.all { it.isStatic() }

    fun compare(step: PairRunner.Step) {
        val solution = step.solution
        val submission = step.submission

        if (solution.stdout.isNotBlank() && solution.stdout != submission.stdout) {
            step.differs.add(PairRunner.Step.Differs.STDOUT)
        }
        if (solution.stderr.isNotBlank() && solution.stderr != submission.stderr) {
            step.differs.add(PairRunner.Step.Differs.STDERR)
        }

        if (step.type == PairRunner.Step.Type.METHOD) {
            if (solution.returned != null && submission.returned != null && solution.returned::class.java.isArray) {
                if (!solution.returned.asArray().contentDeepEquals(submission.returned.asArray())) {
                    step.differs.add(PairRunner.Step.Differs.RETURN)
                }
            } else if (solution.returned != submission.returned) {
                step.differs.add(PairRunner.Step.Differs.RETURN)
            }
        }
        if (solution.threw != submission.threw) {
            step.differs.add(PairRunner.Step.Differs.THREW)
        }
    }

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

data class Result(val returned: Any?, val threw: Throwable?, val stdout: String, val stderr: String)

class PairRunner(val index: Int, val pair: Pair, val methodGenerators: MethodGenerators) {
    var ready = pair.solution.onlyStatic

    var solution: Any? = null
    var submission: Any? = null

    val steps: MutableList<Step> = mutableListOf()

    data class Step(val solution: Result, val submission: Result, val type: Type) {
        enum class Type { CONSTRUCTOR, METHOD }
        enum class Differs { STDOUT, STDERR, RETURN, THREW }

        val differs: MutableSet<Differs> = mutableSetOf()
        val same: Boolean
            get() = differs.isEmpty()
    }

    @Suppress("TooGenericExceptionCaught")
    fun create(): Boolean {
        if (pair.solution.onlyStatic) {
            return true
        }
        val solutionConstructor = pair.solution.solutionConstructors.toList().shuffled().take(1).first()
        val parameters = if (solutionConstructor.parameters.isEmpty()) {
            EmptyParameters
        } else {
            methodGenerators[solutionConstructor]!!.generate()
        }
        val solutionResult = pair.solution.captureOutput {
            unwrapMethodInvocationException {
                solutionConstructor.newInstance(*parameters.solution)
            }
        }
        val submissionConstructor = pair.submissionConstructors[solutionConstructor] ?: error(
            "Answerable couldn't find a submission constructor that should exist"
        )
        val submissionResult = pair.solution.captureOutput {
            unwrapMethodInvocationException {
                submissionConstructor.newInstance(*parameters.submission)
            }
        }
        Step(solutionResult, submissionResult, Step.Type.CONSTRUCTOR).also { step ->
            pair.solution.compare(step)
            if (step.same) {
                solution = step.solution.returned
                submission = step.submission.returned
            }
            ready = step.same
        }
        return ready
    }

    val methodIterator = pair.solution.solutionMethods.cycle()

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

        val solutionResult = pair.solution.captureOutput {
            unwrapMethodInvocationException {
                solutionMethod.invoke(solution, *parameters.solution)
            }
        }

        val submissionResult = pair.solution.captureOutput {
            unwrapMethodInvocationException {
                submissionMethod.invoke(submission, *parameters.submission)
            }
        }

        Step(solutionResult, submissionResult, Step.Type.METHOD).also { step ->
            pair.solution.compare(step)
            ready = step.same
        }
        return ready
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

typealias CaptureOutput = (run: () -> Any?) -> Result

private val outputLock = ReentrantLock()
fun defaultCaptureOutput(run: () -> Any?): Result = outputLock.withLock {
    val original = Pair(System.out, System.err)
    val diverted = Pair(ByteArrayOutputStream(), ByteArrayOutputStream()).also {
        System.setOut(PrintStream(it.first))
        System.setErr(PrintStream(it.second))
    }

    @Suppress("TooGenericExceptionCaught")
    val result: kotlin.Pair<Any?, Throwable?> = try {
        Pair(run(), null)
    } catch (e: Throwable) {
        Pair(null, e)
    }
    System.setOut(original.first)
    System.setErr(original.second)
    return Result(result.first, result.second, diverted.first.toString(), diverted.second.toString())
}

fun unwrapMethodInvocationException(run: () -> Any?): Any? = try {
    run()
} catch (e: InvocationTargetException) {
    throw e.cause ?: error("InvocationTargetException should have a cause")
}