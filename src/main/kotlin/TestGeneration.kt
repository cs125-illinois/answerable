package edu.illinois.cs.cs125.answerable

import edu.illinois.cs.cs125.answerable.TestGenerator.ReceiverGenStrategy.*
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import org.junit.jupiter.api.Assertions.*
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.lang.reflect.InvocationTargetException
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.math.min
import java.lang.Character.UnicodeBlock.*

class TestGenerator(
    val referenceClass: Class<*>,
    name: String = "",
    private val testRunnerArgs: TestRunnerArgs = defaultArgs
) {
    internal val referenceMethod: Method = referenceClass.getReferenceSolutionMethod(name)
    internal val enabledGeneratorAndNextNames: Array<String> =
        referenceMethod.getAnnotation(Solution::class.java).generators

    internal val customVerifier: Method? = referenceClass.getCustomVerifier(name)
    internal val atNextMethod: Method? = referenceClass.getAtNext(enabledGeneratorAndNextNames)

    internal val isStatic = Modifier.isStatic(referenceMethod.modifiers)
    internal val paramTypes: Array<Class<*>> = referenceMethod.parameterTypes

    internal val random: Random = Random(0)
    internal val generators: Map<Class<*>, GenWrapper<*>> = buildGeneratorMap(random)

    internal enum class ReceiverGenStrategy { GENERATOR, NEXT, NONE }
    internal val receiverGenStrategy: ReceiverGenStrategy = when {
        atNextMethod != null              -> NEXT
        referenceClass in generators.keys -> GENERATOR
        isStatic                          -> NONE
        else -> throw AnswerableMisuseException("The reference solution must provide either an @Generator or an @Next method if @Solution is not static.")
    }

    // TODO: enable verification pass
    init {
        verify()
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

        userGens.forEach(generatorMapBuilder::accept)

        return generatorMapBuilder.build()
    }

    internal fun verify() {
        verifyMemberAccess(referenceClass)

        val dryRunOutput = loadSubmission(referenceClass).runTests(0x0403)
        dryRunOutput.forEach {
            if (!it.succeeded) {
                throw AnswerableVerificationException("Testing reference against itself failed on inputs: ${Arrays.deepToString(it.refOutput.args)}")
            }
        }
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
    init {
        defaultGenerators.forEach(this::accept)
        knownGenerators[String::class.java] = lazy { DefaultStringGen(knownGenerators[Char::class.java]!!.value) }
    }

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

    fun build(): Map<Class<*>, GenWrapper<*>> = mapOf(*requiredGenerators.map {
            it to (GenWrapper(knownGenerators[it]?.value ?: throw lazyGenError(it), random))
        }.toTypedArray())

    companion object {
        private val defaultGenerators: List<Pair<Class<*>, Gen<*>>> = listOf(
            Int::class.java     to defaultIntGen,
            Double::class.java  to defaultDoubleGen,
            Float::class.java   to defaultFloatGen,
            Byte::class.java    to defaultByteGen,
            Short::class.java   to defaultShortGen,
            Long::class.java    to defaultLongGen,
            Char::class.java    to defaultCharGen,
            Boolean::class.java to defaultBooleanGen
        )
    }
}

data class TestRunnerArgs(val numTests: Int = 1000)
val defaultArgs = TestRunnerArgs()

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

internal val defaultIntGen = object : Gen<Int> {
    override fun generate(complexity: Int, random: Random): Int {
        var comp = complexity
        if (complexity > Int.MAX_VALUE / 2) {
            comp = Int.MAX_VALUE / 2
        }
        return random.nextInt(comp * 2 + 1) - comp
    }
}

internal val defaultDoubleGen = object : Gen<Double> {
    override fun generate(complexity: Int, random: Random): Double {
        val denom = random.nextDouble() * (1e10 - 1) + 1
        val num = (random.nextDouble() * 2 * complexity * denom) - complexity * denom
        return num / denom
    }
}

internal val defaultFloatGen = object : Gen<Float> {
    override fun generate(complexity: Int, random: Random): Float {
        val denom = random.nextDouble() * (1e10 - 1) + 1
        val num = (random.nextDouble() * 2 * complexity * denom) - complexity * denom
        return (num / denom).toFloat() // if complexity is > 1e38, this stops being uniform
    }
}

internal val defaultByteGen = object : Gen<Byte> {
    override fun generate(complexity: Int, random: Random): Byte {
        return (random.nextInt(complexity * 2 + 1) - complexity).toByte()
    }
}

internal val defaultShortGen = object : Gen<Short> {
    override fun generate(complexity: Int, random: Random): Short {
        return (random.nextInt(complexity * 2 + 1) - complexity).toShort()
    }
}

internal val defaultLongGen = object : Gen<Long> {
    private fun Random.nextLong(bound: Long): Long {
        var bits: Long
        var value: Long
        do {
            bits = (nextLong() shl 1) shr 1
            value = bits % bound
        } while (bits - value + (bound - 1) < 0L)
        return value
    }

    override fun generate(complexity: Int, random: Random): Long {
        return random.nextLong(complexity.toLong() * 4 + 1) - (complexity.toLong() * 2)
    }
}

internal val defaultCharGen = object : Gen<Char> {
    private fun Char.isPrintableAscii(): Boolean = this.toInt() in 32..126

    private fun Char.isPrint(): Boolean = isPrintableAscii() || of(this) in setOf(
        CYRILLIC, CYRILLIC_SUPPLEMENTARY, TAMIL, CURRENCY_SYMBOLS, ARROWS, SUPPLEMENTAL_ARROWS_A,
        ETHIOPIC_EXTENDED, CJK_RADICALS_SUPPLEMENT, KANGXI_RADICALS, KATAKANA_PHONETIC_EXTENSIONS,
        ENCLOSED_CJK_LETTERS_AND_MONTHS, OLD_PERSIAN
    )

    override fun generate(complexity: Int, random: Random): Char {
        return if (random.nextDouble() < min(.15/32 * complexity, .15)) {
            var char: Char
            do {
                char = random.nextInt(0x10000).toChar()
            } while (!char.isPrint())
            char
        } else {
            (random.nextInt(95) + 32).toChar()
        }
    }
}

internal val defaultAsciiGen = object : Gen<Char> {
    override fun generate(complexity: Int, random: Random): Char {
        return (random.nextInt(95) + 32).toChar()
    }
}

internal class DefaultStringGen(private val cGen: Gen<*>) : Gen<String> {
    override fun generate(complexity: Int, random: Random): String {
        val len = random.nextInt(complexity + 1)

        return String((1..len).map { cGen(complexity, random) as Char }.toTypedArray().toCharArray())
    }
}

internal val defaultBooleanGen = object : Gen<Boolean> {
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
                arrayOf(tGen(random.nextInt(complexity + 1), random), *genArray(complexity, length - 1))
            }

        return genArray(complexity, random.nextInt(complexity + 1))
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
) : DefaultSerializable {
    override fun toJson() = defaultToJson()

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

data class TestStep(
    val testNumber: Int,
    val refReceiver: Any?,
    val subReceiver: Any?,
    val succeeded: Boolean,
    val refOutput: TestOutput<Any?>,
    val subOutput: TestOutput<Any?>,
    val assertErr: Throwable?
) : DefaultSerializable {
    override fun toJson() = defaultToJson()
}