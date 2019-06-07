package edu.illinois.cs.cs125.answerable

import edu.illinois.cs.cs125.answerable.TestGenerator.ReceiverGenStrategy.*
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import org.junit.jupiter.api.Assertions.*
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.lang.IllegalStateException
import java.lang.reflect.InvocationTargetException
import java.nio.charset.StandardCharsets
import java.util.*

internal class TestGenerator(
    val referenceClass: Class<*>,
    tag: String = "",
    private val testRunnerArgs: TestRunnerArgs = defaultArgs
) {
    val referenceMethod: Method = referenceClass.getReferenceSolutionMethod(tag)
    val enabledGeneratorAndNextNames: Array<String> =
        referenceMethod.getAnnotation(Solution::class.java).generators

    val customVerifier: Method? = referenceClass.getCustomVerifier(tag)
    val atNextMethod: Method? = referenceClass.getAtNext(enabledGeneratorAndNextNames)

    val isStatic = Modifier.isStatic(referenceMethod.modifiers)
    val paramTypes: Array<Class<*>> = referenceMethod.parameterTypes

    val random: Random = Random(0)
    val generators: Map<Class<*>, GenWrapper<*>> = buildGeneratorMap(random)

    internal enum class ReceiverGenStrategy { GENERATOR, NEXT, NONE }
    val receiverGenStrategy: ReceiverGenStrategy = when {
        atNextMethod != null              -> NEXT
        referenceClass in generators.keys -> GENERATOR
        isStatic                          -> NONE
        else -> throw AnswerableMisuseException("The reference solution must provide either an @Generator or an @Next method if @Solution is not static.")
    }

    // TODO: enable verification pass
    init {
        // verify(referenceClass)
    }

    internal fun buildGeneratorMap(random: Random, submittedClassGenerator: Method? = null): Map<Class<*>, GenWrapper<*>> {
        val types = paramTypes.toSet().let {
            if (!isStatic && atNextMethod == null) {
                it + referenceClass
            } else it
        }

        val generatorMapBuilder = GeneratorMapBuilder(types, random)

        val userGens = referenceClass.getEnabledGenerators(enabledGeneratorAndNextNames).map {
            return@map if (it.returnType == referenceClass && submittedClassGenerator != null) {
                Pair(it.returnType, CustomGen(submittedClassGenerator))
            } else {
                Pair(it.returnType, CustomGen(it))
            }
        }

        userGens.groupBy { it.first }.forEach { gensForType ->
            if (gensForType.value.size > 1) throw AnswerableMisuseException(
                "Found multiple enabled generators for type `${gensForType.key.canonicalName}'."
            )
        }

        val gens = listOf(
            Int::class.java to DefaultIntGen(),
            Double::class.java to DefaultDoubleGen(),
            Float::class.java to DefaultFloatGen(),
            Byte::class.java to DefaultByteGen(),
            Short::class.java to DefaultShortGen(),
            Long::class.java to DefaultLongGen(),
            Char::class.java to DefaultCharGen(),
            Boolean::class.java to DefaultBooleanGen(),
            *userGens.toTypedArray()
        )

        gens.forEach(generatorMapBuilder::accept)

        return generatorMapBuilder.build()
    }

    fun loadSubmission(
        submissionClass: Class<*>,
        testRunnerArgs: TestRunnerArgs = this.testRunnerArgs
    ): TestRunner =
        TestRunner(this, submissionClass, testRunnerArgs)

}

class TestRunner internal constructor(
    testGenerator: TestGenerator,
    private val submissionClass: Class<*>,
    private val testRunnerArgs: TestRunnerArgs
) {
    constructor(
        referenceClass: Class<*>, submissionClass: Class<*>, testRunnerArgs: TestRunnerArgs = defaultArgs
    ) : this(TestGenerator(referenceClass), submissionClass, testRunnerArgs)

    private val referenceClass = testGenerator.referenceClass
    private val referenceMethod = testGenerator.referenceMethod
    private val customVerifier = testGenerator.customVerifier
    private val submissionMethod = submissionClass.findSolutionAttemptMethod(referenceMethod)
    private val paramTypes = testGenerator.paramTypes

    private val randomForReference = testGenerator.random
    private val randomForSubmission = Random(0)

    private val mirrorToStudentClass = mkGeneratorMirrorClass(testGenerator.referenceClass, submissionClass)

    private val referenceGens = testGenerator.generators
    private val submissionGens = mirrorToStudentClass
            .getEnabledGenerators(testGenerator.enabledGeneratorAndNextNames)
            .find { it.returnType == submissionClass }

            .let { testGenerator.buildGeneratorMap(randomForSubmission, it) }
    private val referenceAtNext = testGenerator.atNextMethod
    private val submissionAtNext = mirrorToStudentClass.getAtNext(testGenerator.enabledGeneratorAndNextNames)

    private val receiverGenStrategy = testGenerator.receiverGenStrategy
    private val capturePrint = referenceMethod.getAnnotation(Solution::class.java).prints
    private val isStatic = testGenerator.isStatic

    private fun testWith(iteration: Int, complexity: Int, prevRefReceiver: Any?, prevSubReceiver: Any?): TestStep {
        val refMethodArgs = paramTypes.map { referenceGens[it]?.generate(complexity) }.toTypedArray()
        val subMethodArgs = paramTypes.map { submissionGens[it]?.generate(complexity) }.toTypedArray()

        var refReceiver: Any? = null
        var subReceiver: Any? = null
        var subProxy: Any? = null

        if (!isStatic) {
            refReceiver = mkRefReceiver(iteration, complexity, prevRefReceiver)
            subReceiver = mkSubReceiver(iteration, complexity, prevSubReceiver)

            subProxy = mkProxy(referenceClass, submissionClass, subReceiver!!)
        }

        return test(iteration, refReceiver, subReceiver, subProxy, refMethodArgs, subMethodArgs)
    }

    private fun mkRefReceiver(iteration: Int, complexity: Int, prevRefReceiver: Any?): Any? =
        when (receiverGenStrategy) {
            NONE -> null
            GENERATOR -> referenceGens[referenceClass]?.generate(complexity)
            NEXT -> referenceAtNext?.invoke(null, prevRefReceiver, iteration, randomForReference)
        }

    private fun mkSubReceiver(iteration: Int, complexity: Int, prevSubReceiver: Any?): Any? =
        when (receiverGenStrategy) {
            NONE -> null
            GENERATOR -> submissionGens[referenceClass]?.generate(complexity)
            NEXT -> submissionAtNext?.invoke(null, prevSubReceiver, iteration, randomForSubmission)
        }

    private fun test(
        iteration: Int,
        refReceiver: Any?,
        subReceiver: Any?,
        subProxy: Any?,
        refArgs: Array<Any?>,
        subArgs: Array<Any?>
    ): TestStep {
        fun runOne(receiver: Any?, refCompatibleReceiver: Any?, method: Method, args: Array<Any?>): TestOutput<Any?> {
            var behavior: Behavior

            var threw: Throwable? = null
            val oldOut = System.out
            val oldErr = System.err
            val newOut = ByteArrayOutputStream()
            val newErr = ByteArrayOutputStream()
            var outText: String? = null
            var errText: String? = null
            var output: Any? = null
            if (capturePrint) {
                System.setOut(PrintStream(newOut))
                System.setErr(PrintStream(newErr))
            }
            try {
                output = method(receiver, *args)

                behavior = Behavior.RETURNED
            } catch (e: Throwable) {
                threw = e
                behavior = Behavior.THREW
            } finally {
                if (capturePrint) {
                    System.setOut(oldOut)
                    System.setErr(oldErr)
                    outText = newOut.toString(StandardCharsets.UTF_8)
                    errText = newErr.toString(StandardCharsets.UTF_8)
                    newOut.close()
                    newErr.close()
                }
            }
            return TestOutput(
                typeOfBehavior = behavior,
                receiver = refCompatibleReceiver,
                args = args,
                output = output,
                threw = threw,
                stdOut = outText,
                stdErr = errText
            )
        }

        val refBehavior = runOne(refReceiver, refReceiver, referenceMethod, refArgs)
        val subBehavior = runOne(subReceiver, subProxy, submissionMethod, subArgs)

        var assertErr: Throwable? = null
        try {
            if (customVerifier == null) {
                assertEquals(refBehavior.threw?.javaClass, subBehavior.threw?.javaClass)
                assertEquals(refBehavior.output, subBehavior.output)
                assertEquals(refBehavior.stdOut, subBehavior.stdOut)
                assertEquals(refBehavior.stdErr, subBehavior.stdErr)
            } else {
                if (subProxy != null) {
                    submissionClass.getPublicFields().forEach {
                        referenceClass.getField(it.name).set(subProxy, it.get(subReceiver))
                    }
                }
                customVerifier.invoke(null, refBehavior, subBehavior)
            }
        } catch (ite: InvocationTargetException) {
            assertErr = ite.cause
        }

        return TestStep(
            testNumber = iteration,
            refReceiver = refReceiver,
            subReceiver = subReceiver,
            succeeded = assertErr == null,
            refOutput = refBehavior,
            subOutput = subBehavior,
            assertErr = assertErr
        )
    }

    fun runTests(seed: Long): List<TestStep> {
        setOf(randomForReference, randomForSubmission).forEach { it.setSeed(seed) }

        val testStepList: MutableList<TestStep> = mutableListOf()
        var refReceiver: Any? = null
        var subReceiver: Any? = null
        for (i in 1..1000) {
            val result = testWith(i, i / 20, refReceiver, subReceiver)
            refReceiver = result.refReceiver
            subReceiver = result.subReceiver

            testStepList.add(result)
        }

        return testStepList
    }
}

private class GeneratorMapBuilder(goalTypes: Collection<Class<*>>, private val random: Random) {
    private var knownGenerators: MutableMap<Class<*>, Lazy<Gen<*>>> = mutableMapOf()
    private val requiredGenerators: Set<Class<*>> = goalTypes.toSet().also { it.forEach(this::request) }

    private fun lazyGenError(clazz: Class<*>) = AnswerableMisuseException(
        "A generator for type `${clazz.canonicalName}' was requested, but no generator for that type was found."
    )

    private fun lazyArrayError(clazz: Class<*>) = AnswerableMisuseException(
        "A generator for an array with component type `${clazz.componentType.canonicalName}' was requested, but no generator for that type was found."
    )

    fun accept(pair: Pair<Class<*>, Gen<*>?>) = accept(pair.first, pair.second)

    fun accept(clazz: Class<*>, gen: Gen<*>?) {
        if (gen != null) {
            // kotlin fails to smart cast here even though it says the cast isn't needed
            knownGenerators[clazz] = lazy { gen as Gen<*> }
        }
    }

    private fun request(clazz: Class<*>) {
        if (clazz.isArray) {
            request(clazz.componentType)
            knownGenerators[clazz] =
                lazy {
                    DefaultArrayGen(
                        knownGenerators[clazz.componentType]?.value
                            ?: throw lazyArrayError(clazz)
                    )
                }
        }
    }

    fun build(): Map<Class<*>, GenWrapper<*>> {
        requiredGenerators.forEach {
            knownGenerators[it]?.value ?: throw lazyGenError(it)
        }

        return mapOf(*requiredGenerators.map {
            it to (GenWrapper(knownGenerators[it]?.value ?: throw lazyGenError(it), random))
        }.toTypedArray())
    }
}

data class TestRunnerArgs(val numTests: Int = 1000)
private val defaultArgs = TestRunnerArgs()

internal class GenWrapper<T>(val gen: Gen<T>, private val random: Random) {
    operator fun invoke(complexity: Int) = gen.generate(complexity, random)

    fun generate(complexity: Int) = gen.generate(complexity, random)
}

// So named as to avoid conflict with the @Generator annotation, as that class name is part of the public API and this one is not.
internal interface Gen<out T> {
    operator fun invoke(complexity: Int, random: Random) = generate(complexity, random)

    fun generate(complexity: Int, random: Random): T
}

internal class CustomGen(private val gen: Method) : Gen<Any?> {
    override fun generate(complexity: Int, random: Random): Any? = gen(null, complexity, random)
}

internal class DefaultIntGen : Gen<Int> {
    override fun generate(complexity: Int, random: Random): Int {
        return random.nextInt(complexity * 10 + 1) - (complexity * 5)
    }
}

internal class DefaultDoubleGen : Gen<Double> {
    override fun generate(complexity: Int, random: Random): Double {
        return 0.0
    }
}

internal class DefaultFloatGen : Gen<Float> {
    override fun generate(complexity: Int, random: Random): Float {
        return 0f
    }
}

internal class DefaultByteGen : Gen<Byte> {
    override fun generate(complexity: Int, random: Random): Byte {
        return 0
    }
}

internal class DefaultShortGen : Gen<Short> {
    override fun generate(complexity: Int, random: Random): Short {
        return 0
    }
}

internal class DefaultLongGen : Gen<Long> {
    override fun generate(complexity: Int, random: Random): Long {
        return 0
    }
}

internal class DefaultCharGen : Gen<Char> {
    override fun generate(complexity: Int, random: Random): Char {
        return 'A'
    }
}

internal class DefaultBooleanGen : Gen<Boolean> {
    override fun generate(complexity: Int, random: Random): Boolean = (complexity % 2 != 0)
}

// We can't reify `T`, so we have to inherit from Gen<Array<*>>.
// When using the array generators, we have to be very careful to match the types ourselves.
internal class DefaultArrayGen<T>(private val tGen: Gen<T>) : Gen<Array<*>> {
    override fun generate(complexity: Int, random: Random): Array<*> {
        fun genArray(complexity: Int, length: Int): Array<*> =
            if (length <= 0) {
                arrayOf<Any?>()
            } else {
                arrayOf(tGen(random.nextInt(complexity), random), *genArray(complexity, length - 1))
            }

        return genArray(complexity, complexity)
    }

}

/**
 * A wrapper class used to pass data to custom verification methods.
 */
data class TestOutput<T>(
    val typeOfBehavior: Behavior,
    /** The object that the method was called on. Null if the method is static. */
    val receiver: T?,
    /** The arguments the method was called with */
    val args: Array<Any?>,
    /** The return value of the method. If 'threw' is not null, 'output' is always null. */
    val output: Any?,
    /** The throwable (if any) thrown by the method. Null if nothing was thrown. */
    val threw: Throwable?,
    /** The log of stdOut during the method invocation. Only non-null if the method is static and void. */
    val stdOut: String?,
    /** The log of stdErr during the method invocation. Only non-null if the method is static and void. */
    val stdErr: String?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TestOutput<*>

        if (receiver != other.receiver) return false
        if (!args.contentEquals(other.args)) return false
        if (output != other.output) return false
        if (threw != other.threw) return false
        if (stdOut != other.stdOut) return false
        if (stdErr != other.stdErr) return false

        return true
    }

    override fun hashCode(): Int {
        var result = receiver?.hashCode() ?: 0
        result = 31 * result + args.contentHashCode()
        result = 31 * result + (output?.hashCode() ?: 0)
        result = 31 * result + (threw?.hashCode() ?: 0)
        result = 31 * result + (stdOut?.hashCode() ?: 0)
        result = 31 * result + (stdErr?.hashCode() ?: 0)
        return result
    }
}

enum class Behavior { RETURNED, THREW }

fun <T> TestOutput<T>.toJson(): String {
    val specific = when (this.typeOfBehavior) {
        Behavior.RETURNED -> "  returned: \"$output\""
        Behavior.THREW -> "  threw: \"$threw\""
    }

    val stdOutputs = when (this.stdOut) {
        null -> ""
        else -> """
            |,
            |  stdOut: "$stdOut",
            |  stdErr: "$stdErr"
        """.trimMargin()
    }

    return """
        |{
        |  resultType: "$typeOfBehavior",
        |  receiver: "$receiver",
        |  args: ${args.joinToString(prefix = "[", postfix = "]", transform = ::fixArrayToString)},
        |$specific$stdOutputs
        |}
    """.trimMargin()
}

data class TestStep(
    val testNumber: Int,
    val refReceiver: Any?,
    val subReceiver: Any?,
    val succeeded: Boolean,
    val refOutput: TestOutput<Any?>,
    val subOutput: TestOutput<Any?>,
    val assertErr: Throwable?
)


@Suppress("IMPLICIT_CAST_TO_ANY")
fun TestStep.toJson(): String =
    """
        |{
        |  testNumber: $testNumber,
        |  refReceiver: $refReceiver,
        |  subReceiver: $subReceiver,
        |  succeeded: $succeeded,
        |  refOutput: ${refOutput.toJson()},
        |  subOutput: ${subOutput.toJson()},
        |  assertErr: $assertErr
        |}
    """.trimMargin()

fun List<TestStep>.toJson(): String =
    this.joinToString(prefix = "[", postfix = "]", transform = TestStep::toJson)

fun fixArrayToString(thing: Any?): String = when (thing) {
    is Array<*> -> Arrays.toString(thing)
    else -> thing.toString()
}