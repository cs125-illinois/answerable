package edu.illinois.cs.cs125.answerable

import java.lang.reflect.Method
import java.lang.reflect.Modifier
import org.junit.jupiter.api.Assertions.*
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.lang.IllegalStateException
import java.lang.reflect.InvocationTargetException
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.asJavaRandom
import kotlin.random.asKotlinRandom

internal class TestGenerator(
        private val referenceClass: Class<*>,
        private val submissionClass: Class<*>
) {
    private val reference: Method = referenceClass.getReferenceSolutionMethod()
    private val submission: Method = submissionClass.findSolutionAttemptMethod(reference)
    private val customVerifier: Method? = referenceClass.getCustomVerifier()
    private val nextReceiver: Method? = referenceClass.getAtNext()

    init {
        reference.isAccessible = true
        submission.isAccessible = true
        customVerifier?.isAccessible = true
        nextReceiver?.isAccessible = true
    }

    private val paramTypes: Array<Class<*>> = reference.parameterTypes
    private val generators: Map<Class<*>, Gen<*>> = setUpGenerators()

    private val mirrorReferenceToStudentClass = mkGeneratorMirrorClass(referenceClass, submissionClass)

    private val isStatic = Modifier.isStatic(reference.modifiers)
    private enum class ReceiverGenStrategy { GENERATOR, NEXT, NONE }
    private val receiverGenStrategy: ReceiverGenStrategy = when {
        nextReceiver != null                   -> ReceiverGenStrategy.NEXT
        referenceClass in generators.keys      -> ReceiverGenStrategy.GENERATOR
        Modifier.isStatic(reference.modifiers) -> ReceiverGenStrategy.NONE
        else -> throw AnswerableMisuseException("The reference solution must provide either an @Generator or an @Next method if @Solution is not static.")
    }

    private val random = Random(0)
    // TODO: Is there a cleaner way of handling the Random desync problem?
    private val refReceiverRandom = Random(0)
    private val subReceiverRandom = Random(0)

    private fun setUpGenerators(): Map<Class<*>, Gen<*>> {
        val types = paramTypes.distinct()
        val userGens = reference.declaringClass.getGenerators().map {
            Pair(it.returnType, CustomGen(it))
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

        return mapOf(
                *types.filter { it.isArray }.map { type ->
                    Pair(type, LazyGen { DefaultArrayGen(generators[type.componentType] ?: throw IllegalStateException("An array with component type `${type.componentType}' was requested, but no generator for that type was found.")) })
                }.toTypedArray(),
                *gens.toTypedArray()
        )
    }

    private fun testWith(iteration: Int, complexity: Int, prevRefReceiver: Any?, prevSubReceiver: Any?): TestStep {
        val methodArgs = paramTypes.map { generators[it]?.generate(complexity, random) }.toTypedArray()

        var refReceiver: Any? = null
        var subReceiver: Any? = null
        var subProxy: Any? = null

        if (!isStatic) {
            refReceiver = mkRefReceiver(iteration, complexity, prevRefReceiver)
            subReceiver = mkSubReceiver(iteration, complexity, prevSubReceiver)

            subProxy = mkProxy(referenceClass, submissionClass, subReceiver)
        }

        return test(iteration, refReceiver, subReceiver, subProxy, methodArgs, reference.isStaticVoid())
    }

    fun mkRefReceiver(iteration: Int, complexity: Int, prevRefReceiver: Any?): Any? = when (receiverGenStrategy) {
        ReceiverGenStrategy.NONE -> null
        ReceiverGenStrategy.GENERATOR -> generators[referenceClass]?.generate(complexity, refReceiverRandom)
        ReceiverGenStrategy.NEXT -> nextReceiver?.invoke(null, prevRefReceiver, iteration, refReceiverRandom)
    }
    fun mkSubReceiver(iteration: Int, complexity: Int, prevSubReceiver: Any?): Any? = when (receiverGenStrategy) {
        ReceiverGenStrategy.NONE -> null
        ReceiverGenStrategy.GENERATOR -> {
            // TODO: Replace with generators[...]...
            mirrorReferenceToStudentClass.methods.first { it.isAnnotationPresent(Generator::class.java) && it.returnType == submissionClass }(null, complexity, subReceiverRandom)
        }
        ReceiverGenStrategy.NEXT -> {
            mirrorReferenceToStudentClass.methods.first { it.isAnnotationPresent(Next::class.java) && it.returnType == submissionClass }(null, prevSubReceiver, iteration, subReceiverRandom)
        }
    }

    fun test(iteration: Int, refReceiver: Any?, subReceiver: Any?, subProxy: Any?, args: Array<Any?>, capturePrint: Boolean): TestStep {
        fun runOne(receiver: Any?, refCompatibleReceiver: Any?, method: Method): TestOutput<Any?> {
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

                    behavior = Behavior.PRINTED
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

        val refBehavior = runOne(refReceiver, refReceiver, reference)
        val subBehavior = runOne(subReceiver, subProxy, submission)

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
                inputs = args.toList(),
                succeeded = assertErr == null,
                refThrew = refBehavior.threw,
                subThrew = subBehavior.threw,
                assertErr = assertErr
        )
    }

    fun runTests(seed: Long): List<TestStep> {
        setOf(random, refReceiverRandom, subReceiverRandom).forEach { it.setSeed(seed) }

        val testStepList: MutableList<TestStep> = mutableListOf()
        var refReceiver: Any? = null
        var subReceiver: Any? = null
        for (i in 1 .. 1000) {
            val result = testWith(i, i / 20, refReceiver, subReceiver)
            refReceiver = result.refReceiver
            subReceiver = result.subReceiver

            testStepList.add(result)
        }

        return testStepList
    }
}

// So named as to avoid conflict with the @Generator annotation, as that class name is part of the public API and this one is not.
internal interface Gen<out T> {
    fun generate(complexity: Int, random: Random): T
}

internal class CustomGen(private val gen: Method) : Gen<Any?> {
    override fun generate(complexity: Int, random: Random): Any? = gen(null, complexity, random)
}

internal class LazyGen<T>(private val genSupplier: () -> Gen<T>) : Gen<T> {
    private var gen: Gen<T>? = null

    override fun generate(complexity: Int, random: Random): T {
        if (gen == null) {
            gen = genSupplier()
        }

        return gen!!.generate(complexity, random)
    }
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
                    arrayOf(tGen.generate(random.nextInt(complexity), random), *genArray(complexity, length - 1))
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
enum class Behavior { RETURNED, PRINTED, THREW, TIMED_OUT }

data class TestStep(
        val testNumber: Int,
        val refReceiver: Any?,
        val subReceiver: Any?,
        val inputs: List<*>,
        val succeeded: Boolean,
        val refThrew: Throwable?,
        val subThrew: Throwable?,
        val assertErr: Throwable?
)


@Suppress("IMPLICIT_CAST_TO_ANY")
fun TestStep.toJson(): String =
        """
        |{
        |  testNumber: $testNumber,
        |  refReceiver: $refReceiver,
        |  subReceiver: $subReceiver,
        |  inputs: ${inputs.joinToString(prefix = "[", postfix = "]") { when (it) { is Array<*> -> Arrays.toString(it); else -> it.toString()} }},
        |  succeeded: $succeeded,
        |  refThrew: $refThrew,
        |  subThrew: $subThrew,
        |  assertErr: $assertErr
        |}
    """.trimMargin()

fun List<TestStep>.toJson(): String =
        this.joinToString(prefix = "[", postfix = "]", transform = TestStep::toJson)