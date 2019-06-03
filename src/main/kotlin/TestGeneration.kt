package edu.illinois.cs.cs125.answerable

import java.lang.reflect.Method
import java.lang.reflect.Modifier
import org.junit.jupiter.api.Assertions.*
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.lang.IllegalStateException
import java.nio.charset.StandardCharsets
import java.util.*

internal class TestGenerator(
    private val referenceClass: Class<*>,
    private val submissionClass: Class<*>
) {
    private val reference: Method = referenceClass.getReferenceSolutionMethod()
    private val submission: Method = submissionClass.findSolutionAttemptMethod(reference)
    private val customVerifier: Method? = referenceClass.getCustomVerifier()

    init {
        reference.isAccessible = true
        submission.isAccessible = true
        customVerifier?.isAccessible = true
    }

    private val paramTypes: Array<out Class<*>> = reference.parameterTypes
    private val generators: Map<Class<*>, Gen<*>> = setUpGenerators()

    private val isStatic = Modifier.isStatic(reference.modifiers)

    private val random = Random(0)

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

    private fun testWith(iteration: Int, complexity: Int): TestStep {
        val args = paramTypes.map { generators[it]?.generate(complexity, random) }.toTypedArray()

        val refReceiver: Any? = if (isStatic) null else null // TODO: We have to solve the receiver subclassing problem
        val subReceiver: Any? = if (isStatic) null else null // TODO: as above

        return if (reference.isStaticVoid()) {
            testPrintedOutput(iteration, refReceiver, subReceiver, args)
        } else {
            testStandard(iteration, refReceiver, subReceiver, args)
        }
    }

    private fun testPrintedOutput(iteration: Int, refReceiver: Any?, subReceiver: Any?, args: Array<Any?>): TestStep {
        var refThrew: Throwable? = null
        var subThrew: Throwable? = null

        var assertErr: Throwable? = null
        try {
            var refOut: String? = null
            var refErr: String? = null
            var subOut: String? = null
            var subErr: String? = null

            val oldOut = System.out
            val oldErr = System.err
            try {
                val newOut = ByteArrayOutputStream()
                System.setOut(PrintStream(newOut))

                val newErr = ByteArrayOutputStream()
                System.setErr(PrintStream(newErr))

                reference(refReceiver, *args)

                refOut = newOut.toString(StandardCharsets.UTF_8)
                refErr = newErr.toString(StandardCharsets.UTF_8)

                newOut.close()
                newErr.close()
            } catch (t: Throwable) {
                refThrew = t
            }
            try {
                val newOut = ByteArrayOutputStream()
                System.setOut(PrintStream(newOut))

                val newErr = ByteArrayOutputStream()
                System.setErr(PrintStream(newErr))

                submission(refReceiver, *args)

                subOut = newOut.toString(StandardCharsets.UTF_8)
                subErr = newErr.toString(StandardCharsets.UTF_8)

                newOut.close()
                newErr.close()
            } catch (t: Throwable) {
                subThrew = t
            }

            System.setOut(oldOut)
            System.setErr(oldErr)

            if (customVerifier == null) {
                assertEquals(refThrew?.javaClass, subThrew?.javaClass)
                assertEquals(refOut, subOut)
                assertEquals(refErr, subErr)
            } else {
                customVerifier.invoke(null,
                    TestOutput(
                        receiver = refReceiver,
                        args = args,
                        output = null,
                        threw = refThrew,
                        stdOut = refOut,
                        stdErr = refErr
                    ),
                    TestOutput(
                        receiver = subReceiver,
                        args = args,
                        output = null,
                        threw = subThrew,
                        stdOut = subOut,
                        stdErr = subErr
                    )
                )
            }
        } catch (t: Throwable) {
            assertErr = t
        }

        if (refThrew == null && subThrew != null) {
            throw subThrew
        }

        return TestStep(
            testNumber = iteration,
            refReceiver = refReceiver,
            subReceiver = subReceiver,
            inputs = args.toList(),
            succeeded = assertErr == null,
            assertErr = assertErr
        )
    }
    private fun testStandard(iteration: Int, refReceiver: Any?, subReceiver: Any?, args: Array<Any?>): TestStep {
        var refThrew: Throwable? = null
        var subThrew: Throwable? = null

        var assertErr: Throwable? = null
        try {
            var refOutput: Any? = null
            var subOutput: Any? = null
            try {
                refOutput = reference(refReceiver, *args)
            } catch (t: Throwable) {
                refThrew = t
            }
            try {
                subOutput = submission(subReceiver, *args)
            } catch (t: Throwable) {
                subThrew = t
            }

            if (customVerifier == null) {
                assertEquals(refThrew?.javaClass, subThrew?.javaClass)
                assertEquals(refOutput, subOutput)
            } else {
                customVerifier.invoke(null,
                    TestOutput(
                        receiver = refReceiver,
                        args = args,
                        output = refOutput,
                        threw = refThrew,
                        stdOut = null,
                        stdErr = null
                    ),
                    TestOutput(
                        receiver = subReceiver,
                        args = args,
                        output = subOutput,
                        threw = subThrew,
                        stdOut = null,
                        stdErr = null
                    )
                )
            }
        } catch (t: Throwable) {
            assertErr = t
        }

        if (refThrew == null && subThrew != null) {
            throw subThrew
        }

        return TestStep(
            testNumber = iteration,
            refReceiver = refReceiver,
            subReceiver = subReceiver,
            inputs = args.toList(),
            succeeded = assertErr == null,
            assertErr = assertErr
        )
    }

    fun runTests(seed: Long): List<TestStep> {
        random.setSeed(seed)
        return (1..1000).map { iter -> testWith(iter, iter / 20) }
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
        return 0
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

data class TestStep(
    val testNumber: Int,
    val refReceiver: Any?,
    val subReceiver: Any?,
    val inputs: List<*>,
    val succeeded: Boolean,
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
        |  assertErr: $assertErr
        |}
    """.trimMargin()

fun List<TestStep>.toJson(): String =
        this.joinToString(prefix = "[", postfix = "]", transform = TestStep::toJson)