package edu.illinois.cs.cs125.answerable

import java.lang.reflect.Method
import java.lang.reflect.Modifier
import org.junit.jupiter.api.Assertions.*
import org.opentest4j.AssertionFailedError
import java.util.*

internal class TestGenerator(
    private val reference: Method,
    private val submission: Method,
    private val customVerifier: Method?
) {
    private val paramTypes: Array<out Class<*>> = reference.parameterTypes
    private val generators: Map<Class<*>, Gen<*>> = setUpGenerators()

    private val isStatic = Modifier.isStatic(reference.modifiers)

    private val random = Random(0)

    private fun setUpGenerators(): Map<Class<*>, Gen<*>> {
        val types = paramTypes.distinct()
        val userGens = reference.declaringClass.getGenerators().map { Pair(it.returnType, CustomGen(it)) }

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
            *gens.filter { it.first.typeName in types.map { it.typeName } }.toTypedArray()
        )
    }

    private fun testWith(iteration: Int, complexity: Int): TestStep {
        val args = paramTypes.map { generators[it]?.generate(complexity, random) }.toTypedArray()

        val refReceiver: Any? = if (isStatic) null else null // TODO: We have to solve the receiver subclassing problem
        val subReceiver: Any? = if (isStatic) null else null // TODO: as above

        var threw: Throwable? = null

        try {
            if (customVerifier == null) {
                val refOutput = reference(refReceiver, *args)
                val subOutput = submission(subReceiver, *args)

                assertEquals(refOutput, subOutput)
            }
        } catch (t: AssertionFailedError) {
            threw = t
        }

        return TestStep(
            testNumber = iteration,
            refReceiver = refReceiver,
            subReceiver = subReceiver,
            inputs = args.toList(),
            succeeded = threw == null,
            threw = threw
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

data class TestStep(
    val testNumber: Int,
    val refReceiver: Any?,
    val subReceiver: Any?,
    val inputs: List<*>,
    val succeeded: Boolean,
    val threw: Throwable?
)


fun TestStep.toJson(): String =
    """
        |{
        |  testNumber: $testNumber,
        |  refReceiver: $refReceiver,
        |  subReceiver: $subReceiver,
        |  inputs: $inputs,
        |  succeeded: $succeeded,
        |  threw: $threw
        |}
    """.trimMargin()

fun List<TestStep>.toJson(): String =
        this.joinToString(prefix = "[", postfix = "]", transform = TestStep::toJson)