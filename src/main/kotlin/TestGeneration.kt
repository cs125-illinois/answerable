package edu.illinois.cs.cs125.answerable

import edu.illinois.cs.cs125.answerable.TestGenerator.ReceiverGenStrategy.*
import edu.illinois.cs.cs125.answerable.api.DefaultSerializable
import edu.illinois.cs.cs125.answerable.api.defaultToJson
import edu.illinois.cs.cs125.answerable.typeManagement.*
import edu.illinois.cs.cs125.answerable.typeManagement.mkGeneratorMirrorClass
import edu.illinois.cs.cs125.answerable.typeManagement.mkProxy
import edu.illinois.cs.cs125.answerable.typeManagement.verifyMemberAccess
import org.junit.jupiter.api.Assertions.*
import org.opentest4j.AssertionFailedError
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.math.min
import java.lang.Character.UnicodeBlock.*
import java.lang.IllegalStateException
import java.lang.reflect.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.random.asKotlinRandom
import java.lang.reflect.Array as ReflectArray

class TestGenerator(
    val referenceClass: Class<*>,
    val solutionName: String = "",
    private val testRunnerArgs: TestRunnerArgs = defaultArgs
) {
    internal val referenceMethod: Method? = referenceClass.getReferenceSolutionMethod(solutionName)
    internal val enabledNames: Array<String> =
        referenceMethod?.getAnnotation(Solution::class.java)?.enabled ?: arrayOf()

    internal val customVerifier: Method? = referenceClass.getCustomVerifier(solutionName)
    init {
        if (referenceMethod == null) {
            if (customVerifier == null) {
                throw AnswerableMisuseException("No @Solution annotation or @Verify annotation with name `$solutionName' was found.")
            } else if (!customVerifier.getAnnotation(Verify::class.java)!!.standalone) {
                throw AnswerableMisuseException("No @Solution annotation with name `$solutionName' was found.\nPerhaps you meant" +
                        "to make verifier `${MethodData(customVerifier)}' standalone?")
            }
        }
    }
    internal val atNextMethod: Method? = referenceClass.getAtNext(enabledNames)

    internal val isStatic = referenceMethod?.let { Modifier.isStatic(it.modifiers) } ?: false
    internal val paramTypes: Array<Type> = referenceMethod?.genericParameterTypes ?: arrayOf()
    internal val paramTypesWithReceiver: Array<Type> = arrayOf(referenceClass, *paramTypes)

    internal val random: Random = Random(0)
    internal val generators: Map<Type, GenWrapper<*>> = buildGeneratorMap(random)
    internal val edgeCases: Map<Type, ArrayWrapper?> = getEdgeCases(referenceClass, paramTypesWithReceiver)
    internal val simpleCases: Map<Type, ArrayWrapper?> = getSimpleCases(referenceClass, paramTypesWithReceiver)

    internal val timeout = referenceMethod?.getAnnotation(Timeout::class.java)?.timeout
        ?: (customVerifier?.getAnnotation(Timeout::class.java)?.timeout ?: 0)

    internal enum class ReceiverGenStrategy { GENERATOR, NEXT, NONE }
    internal val receiverGenStrategy: ReceiverGenStrategy = when {
        atNextMethod != null              -> NEXT
        referenceClass in generators.keys -> GENERATOR
        isStatic                          -> NONE
        else -> throw AnswerableMisuseException("The reference solution must provide either an @Generator or an @Next method if @Solution is not static.")
    }

    init {
        verify()
    }

    internal fun buildGeneratorMap(random: Random, submittedClassGenerator: Method? = null): Map<Type, GenWrapper<*>> {
        val types = paramTypes.toSet().let {
            if (!isStatic && atNextMethod == null) {
                it + referenceClass
            } else it
        }

        val generatorMapBuilder = GeneratorMapBuilder(types, random)

        val userGens = referenceClass.getEnabledGenerators(enabledNames).map {
            return@map if (it.returnType == referenceClass && submittedClassGenerator != null) {
                Pair(it.genericReturnType, CustomGen(submittedClassGenerator))
            } else {
                Pair(it.genericReturnType, CustomGen(it))
            }
        }

        userGens.groupBy { it.first }.forEach { gensForType ->
            if (gensForType.value.size > 1) throw AnswerableMisuseException(
                "Found multiple enabled generators for type `${gensForType.key.sourceName}'."
            )
        }

        userGens.forEach(generatorMapBuilder::accept)

        return generatorMapBuilder.build()
    }

    private fun getEdgeCases(clazz: Class<*>, types: Array<Type>): Map<Type, ArrayWrapper?> {
        val all = defaultEdgeCases + clazz.getEnabledEdgeCases(enabledNames)
        return mapOf(*types.map { it to all[it] }.toTypedArray())
    }
    private fun getSimpleCases(clazz: Class<*>, types: Array<Type>): Map<Type, ArrayWrapper?> {
        val all = defaultSimpleCases + clazz.getEnabledSimpleCases(enabledNames)
        return mapOf(*types.map { it to all[it] }.toTypedArray())
    }

    internal fun verify() {
        verifyMemberAccess(referenceClass)

        val dryRun = { loadSubmission(referenceClass).runTests(0x0403) }
        val dryRunOutput: TestRunOutput

        try {
            dryRunOutput = Executors.newSingleThreadExecutor().submit(dryRun)[10000, TimeUnit.MILLISECONDS]
        } catch (e: TimeoutException) {
            throw AnswerableVerificationException(
                "Testing reference against itself timed out (10s)."
            )
        } catch (e: ExecutionException) {
            if (e.cause != null) {
                throw e.cause!!
            } else {
                throw e
            }
        }

        dryRunOutput.testSteps.forEach {
            if (!it.succeeded) {
                throw AnswerableVerificationException(
                    "Testing reference against itself failed on inputs: ${Arrays.deepToString(
                        it.refOutput.args
                    )}"
                ).initCause(it.assertErr)
            }
        }
    }

    fun loadSubmission(
        submissionClass: Class<*>,
        testRunnerArgs: TestRunnerArgs = this.testRunnerArgs
    ): TestRunner {
        val cda = ClassDesignAnalysis(solutionName, referenceClass, submissionClass).runSuite()
        val cdaPassed = cda.all { ao -> ao.result is Matched }

        return if (cdaPassed || referenceClass == submissionClass) {
            PassedClassDesignRunner(this, submissionClass, cda, testRunnerArgs)
        } else {
            FailedClassDesignTestRunner(solutionName, submissionClass, cda)
        }
    }

}

interface TestRunner {
    fun runTests(seed: Long, testRunnerArgs: TestRunnerArgs): TestRunOutput
    fun runTests(seed: Long): TestRunOutput
}

open class PassedClassDesignRunner internal constructor(
    testGenerator: TestGenerator,
    private val submissionClass: Class<*>,
    private val cachedClassDesignAnalysisResult: List<AnalysisOutput> = listOf(),
    private val testRunnerArgs: TestRunnerArgs
) : TestRunner {
    constructor(
        referenceClass: Class<*>, submissionClass: Class<*>, cdaResult: List<AnalysisOutput> = listOf(), testRunnerArgs: TestRunnerArgs = defaultArgs
    ) : this(TestGenerator(referenceClass), submissionClass, cdaResult, testRunnerArgs)

    private val solutionName = testGenerator.solutionName

    private val referenceClass = testGenerator.referenceClass
    private val referenceMethod = testGenerator.referenceMethod
    private val customVerifier = testGenerator.customVerifier
    private val submissionMethod = submissionClass.findSolutionAttemptMethod(referenceMethod)
    private val paramTypes = testGenerator.paramTypes
    private val paramTypesWithReceiver = testGenerator.paramTypesWithReceiver

    private val testRunnerRandom = Random(0)
    private val randomForReference = testGenerator.random
    private val randomForSubmission = Random(0)

    private val mirrorToStudentClass = mkGeneratorMirrorClass(referenceClass, submissionClass)

    private val referenceEdgeCases = testGenerator.edgeCases
    private val referenceSimpleCases = testGenerator.simpleCases
    private val submissionEdgeCases: Map<Type, ArrayWrapper?> = referenceEdgeCases
        // replace reference class cases with mirrored cases
        // the idea is that each map takes `paramTypes` to the correct generator/cases
        .toMutableMap().apply {
            replace(
                referenceClass,
                mirrorToStudentClass.getEnabledEdgeCases(testGenerator.enabledNames)[submissionClass]
            )
        }
    private val submissionSimpleCases: Map<Type, ArrayWrapper?> = referenceSimpleCases
        // replace reference class cases with mirrored cases
        .toMutableMap().apply {
            replace(
                referenceClass,
                mirrorToStudentClass.getEnabledSimpleCases(testGenerator.enabledNames)[submissionClass]
            )
        }

    private val numEdgeCombinations = calculateNumCases(referenceEdgeCases)
    private val numSimpleCombinations = calculateNumCases(referenceSimpleCases)

    private val referenceGens = testGenerator.generators
    private val submissionGens = mirrorToStudentClass
        .getEnabledGenerators(testGenerator.enabledNames)
        .find { it.returnType == submissionClass }
        .let { testGenerator.buildGeneratorMap(randomForSubmission, it) }
    private val referenceAtNext = testGenerator.atNextMethod
    private val submissionAtNext = mirrorToStudentClass.getAtNext(testGenerator.enabledNames)

    private val receiverGenStrategy = testGenerator.receiverGenStrategy
    private val capturePrint = referenceMethod?.getAnnotation(Solution::class.java)?.prints ?: false
    private val isStatic = testGenerator.isStatic
    private val timeout = testGenerator.timeout

    private fun calculateNumCases(cases: Map<Type, ArrayWrapper?>): Int =
        paramTypesWithReceiver.foldIndexed(1) { idx, acc, type ->
            cases[type]?.let { cases: ArrayWrapper ->
                (if (idx == 0) ((cases as? AnyArrayWrapper<*>)?.sizeWithoutNull ?: 1) else cases.size).let {
                    return@foldIndexed acc * it
                }
            } ?: acc
        }

    private fun calculateCase(
        index: Int,
        total: Int,
        cases: Map<Type, ArrayWrapper?>,
        backups: Map<Type, GenWrapper<*>>
    ): Array<Any?> {
        var segmentSize = total
        var segmentIndex = index

        val case = Array<Any?>(paramTypesWithReceiver.size) { null }
        for (i in paramTypesWithReceiver.indices) {
            val type = paramTypesWithReceiver[i]
            val typeCases = cases[type]

            if (i == 0) { // receiver
                if (typeCases == null) {
                    case[0] = null
                    continue
                }
                typeCases as? AnyArrayWrapper<*> ?: throw IllegalStateException("Answerable thinks a receiver type is primitive. Please report a bug.")
                val typeCasesArr = typeCases.arr
                val typeCasesLst = typeCasesArr.filter { it != null } // receivers can't be null

                if (typeCasesLst.isEmpty()) {
                    case[0] = null
                    continue
                }

                val typeNumCases = typeCasesLst.size
                segmentSize /= typeNumCases
                case[i] = typeCases[segmentIndex / segmentSize]
                segmentIndex %= segmentSize

            } else { // non-receiver

                if (typeCases == null || typeCases.size == 0) {
                    case[i] = backups[type]?.generate(0)
                    continue
                }

                val typeNumCases = typeCases.size
                segmentSize /= typeNumCases
                case[i] = typeCases[segmentIndex / segmentSize]
                segmentIndex %= segmentSize
            }
        }
        return case
    }

    private fun mkSimpleEdgeMixedCase(
        edges: Map<Type, ArrayWrapper?>,
        simples: Map<Type, ArrayWrapper?>,
        backups: Map<Type, GenWrapper<*>>,
        random: Random
    ): Array<Any?> {
        val case = Array<Any?>(paramTypes.size) { null }
        for (i in paramTypes.indices) {
            val edge = random.nextInt(2) == 0
            var simple = !edge
            val type = paramTypes[i]

            if (edge) {
                if (edges[type] != null && edges.getValue(type)!!.size != 0) {
                    case[i] = edges.getValue(type)!!.random(random)
                } else {
                    simple = true
                }
            }
            if (simple) {
                if (simples[type] != null && simples.getValue(type)!!.size != 0) {
                    case[i] = simples.getValue(type)!!.random(random)
                } else {
                    case[i] = backups[type]?.generate(if (edge) 0 else 2)
                }
            }
        }

        return case
    }

    private fun mkGeneratedMixedCase(
        edges: Map<Type, ArrayWrapper?>,
        simples: Map<Type, ArrayWrapper?>,
        gens: Map<Type, GenWrapper<*>>,
        complexity: Int,
        random: Random
    ): Array<Any?> {
        val case = Array<Any?>(paramTypes.size) { null }

        for (i in paramTypes.indices) {
            val type = paramTypes[i]
            var choice = random.nextInt(3)
            if (choice == 0) {
                if (edges[type] != null && edges.getValue(type)!!.size != 0) {
                    case[i] = edges.getValue(type)!!.random(random)
                } else {
                    choice = 2
                }
            }
            if (choice == 1) {
                if (simples[type] != null && edges.getValue(type)!!.size != 0) {
                    case[i] = simples.getValue(type)!!.random(random)
                } else {
                    choice = 2
                }
            }
            if (choice == 2) {
                case[i] = gens[type]?.generate(complexity)
            }
        }

        return case
    }

    private fun testWith(
        iteration: Int,
        refReceiver: Any?,
        subReceiver: Any?,
        refMethodArgs: Array<Any?>,
        subMethodArgs: Array<Any?>
    ): TestStep {

        var subProxy: Any? = null

        if (!isStatic) {
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
        fun runOne(receiver: Any?, refCompatibleReceiver: Any?, method: Method?, args: Array<Any?>): TestOutput<Any?> {
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
            if (method != null) {
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
            } else {
                behavior = Behavior.VERIFY_ONLY
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
        } catch (e: AssertionFailedError) {
            assertErr = e
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

    override fun runTests(seed: Long, testRunnerArgs: TestRunnerArgs): TestRunOutput {
        val numTests = testRunnerArgs.numTests

        val numEdgeCaseTests = if (referenceEdgeCases.values.all { it.isNullOrEmpty() }) 0 else min(
            testRunnerArgs.maxOnlyEdgeCaseTests,
            numEdgeCombinations
        )
        val edgeExhaustive = numEdgeCombinations <= testRunnerArgs.maxOnlyEdgeCaseTests
        val numSimpleCaseTests = if (referenceSimpleCases.values.all { it.isNullOrEmpty() }) 0 else min(
            testRunnerArgs.maxOnlySimpleCaseTests,
            numSimpleCombinations
        )
        val simpleExhaustive = numSimpleCombinations <= testRunnerArgs.maxOnlySimpleCaseTests
        val numSimpleEdgeMixedTests = testRunnerArgs.numSimpleEdgeMixedTests
        val numAllGeneratedTests = testRunnerArgs.numAllGeneratedTests
        val numGeneratedMixedTests = numTests -
                numAllGeneratedTests -
                numSimpleEdgeMixedTests -
                numSimpleCaseTests -
                numEdgeCaseTests

        val edgeCaseUpperBound = numEdgeCaseTests
        val simpleCaseUpperBound = numEdgeCaseTests + numSimpleCaseTests
        val simpleEdgeMixedUpperBound = simpleCaseUpperBound + numSimpleEdgeMixedTests
        val generatedMixedUpperBound = simpleEdgeMixedUpperBound + numGeneratedMixedTests
        val allGeneratedUpperBound = generatedMixedUpperBound + numAllGeneratedTests

        if (allGeneratedUpperBound != numTests) {
            throw IllegalStateException("Main testing loop block size calculation failed. Please report a bug.")
        }

        val testStepList: MutableList<TestStep> = mutableListOf()
        val testingBlockCounts = object {
            var edgeTests: Int = 0
            var simpleTests: Int = 0
            var simpleEdgeMixedTests: Int = 0
            var generatedMixedTests: Int = 0
            var allGeneratedTests: Int = 0

            val numTests: Int
                get() = edgeTests + simpleTests + simpleEdgeMixedTests + generatedMixedTests + allGeneratedTests
        }

        fun timedTestingPortion() {
            var refReceiver: Any? = null
            var subReceiver: Any? = null

            var block: Int

            for (i in 1..testRunnerArgs.numTests) {
                val refMethodArgs: Array<Any?>
                val subMethodArgs: Array<Any?>
                when (i) {
                    in 1 .. edgeCaseUpperBound -> {
                        block = 0

                        // if we can't exhaust the cases, duplicates are less impactful
                        val idx = if (edgeExhaustive) (i - 1) else testRunnerRandom.nextInt(numEdgeCombinations)

                        val refCase = calculateCase(idx, numEdgeCombinations, referenceEdgeCases, referenceGens)
                        val subCase = calculateCase(idx, numEdgeCombinations, submissionEdgeCases, submissionGens)

                        refMethodArgs = refCase.slice(1..refCase.indices.last).toTypedArray()
                        subMethodArgs = subCase.slice(1..subCase.indices.last).toTypedArray()

                        refReceiver = if (refCase[0] != null) refCase[0] else mkRefReceiver(i, 0, refReceiver)
                        subReceiver = if (subCase[0] != null) subCase[0] else mkSubReceiver(i, 0, subReceiver)

                    }
                    in (edgeCaseUpperBound + 1) .. simpleCaseUpperBound -> {
                        block = 1

                        val idxInSegment = i - edgeCaseUpperBound - 1
                        val idx = if (simpleExhaustive) idxInSegment else testRunnerRandom.nextInt(numSimpleCombinations)

                        val refCase = calculateCase(idx, numSimpleCombinations, referenceSimpleCases, referenceGens)
                        val subCase = calculateCase(idx, numSimpleCombinations, submissionSimpleCases, submissionGens)

                        refMethodArgs = refCase.slice(1..refCase.indices.last).toTypedArray()
                        subMethodArgs = subCase.slice(1..subCase.indices.last).toTypedArray()

                        refReceiver = if (refCase[0] != null) refCase[0] else mkRefReceiver(i, 0, refReceiver)
                        subReceiver = if (subCase[0] != null) subCase[0] else mkSubReceiver(i, 0, subReceiver)

                    }
                    in (simpleCaseUpperBound + 1) .. simpleEdgeMixedUpperBound -> {
                        block = 2

                        refReceiver = mkRefReceiver(i, 2, refReceiver)
                        subReceiver = mkSubReceiver(i, 2, subReceiver)

                        refMethodArgs = mkSimpleEdgeMixedCase(referenceEdgeCases, referenceSimpleCases, referenceGens, randomForReference)
                        subMethodArgs = mkSimpleEdgeMixedCase(submissionEdgeCases, submissionSimpleCases, submissionGens, randomForSubmission)
                    }
                    in (simpleEdgeMixedUpperBound + 1) .. generatedMixedUpperBound -> {
                        block = 3

                        val idxInSegment = i - simpleEdgeMixedUpperBound - 1
                        val comp = (testRunnerArgs.maxComplexity * idxInSegment) / numGeneratedMixedTests

                        refReceiver = mkRefReceiver(i, comp, refReceiver)
                        subReceiver = mkSubReceiver(i, comp, subReceiver)

                        refMethodArgs = mkGeneratedMixedCase(referenceEdgeCases, referenceSimpleCases, referenceGens, comp, randomForReference)
                        subMethodArgs = mkGeneratedMixedCase(submissionEdgeCases, submissionSimpleCases, submissionGens, comp, randomForSubmission)
                    }
                    in (generatedMixedUpperBound + 1) .. numTests -> {
                        block = 4

                        val idxInSegment = i - generatedMixedUpperBound - 1
                        val comp = (testRunnerArgs.maxComplexity * idxInSegment) / numAllGeneratedTests

                        refReceiver = mkRefReceiver(i, comp, refReceiver)
                        subReceiver = mkSubReceiver(i, comp, subReceiver)

                        refMethodArgs = paramTypes.map { referenceGens[it]?.generate(comp) }.toTypedArray()
                        subMethodArgs = paramTypes.map { submissionGens[it]?.generate(comp) }.toTypedArray()
                    }
                    else ->
                        throw IllegalStateException(
                            "Main test loop index not within bounds of any main testing block. Please report a bug."
                        )
                }

                val result = testWith(i, refReceiver, subReceiver, refMethodArgs, subMethodArgs)
                testStepList.add(result)

                when (block) {
                    0 -> testingBlockCounts.edgeTests++
                    1 -> testingBlockCounts.simpleTests++
                    2 -> testingBlockCounts.simpleEdgeMixedTests++
                    3 -> testingBlockCounts.generatedMixedTests++
                    4 -> testingBlockCounts.allGeneratedTests++
                }
            }
        }

        setOf(randomForReference, randomForSubmission, testRunnerRandom).forEach { it.setSeed(seed) }

        var timedOut = false

        val startTime: Long = System.currentTimeMillis()

        if (timeout == 0L) {
            timedTestingPortion()
        } else {
            try {
                Executors.newSingleThreadExecutor().submit(::timedTestingPortion)[timeout, TimeUnit.MILLISECONDS]
            } catch (e: TimeoutException) {
                timedOut = true
            }
        }

        val endTime: Long = System.currentTimeMillis()

        return TestRunOutput(
            seed = seed,
            testedClass = referenceClass,
            solutionName = solutionName,
            startTime = startTime,
            endTime = endTime,
            timedOut = timedOut,
            numTests = testingBlockCounts.numTests,
            numEdgeCaseTests = testingBlockCounts.edgeTests,
            numSimpleCaseTests = testingBlockCounts.simpleTests,
            numSimpleAndEdgeCaseTests = testingBlockCounts.simpleEdgeMixedTests,
            numMixedTests = testingBlockCounts.generatedMixedTests,
            numAllGeneratedTests = testingBlockCounts.allGeneratedTests,
            classDesignAnalysisResult = cachedClassDesignAnalysisResult,
            testSteps = testStepList
        )
    }
    override fun runTests(seed: Long) = runTests(seed, this.testRunnerArgs) // to expose the overload to Java



}

class FailedClassDesignTestRunner(
    private val solutionName: String,
    private val submissionClass: Class<*>,
    private val failedCDAResult: List<AnalysisOutput>
) : TestRunner {
    override fun runTests(seed: Long): TestRunOutput =
            TestRunOutput(
                seed = seed,
                testedClass = submissionClass,
                solutionName = solutionName,
                startTime = System.currentTimeMillis(),
                endTime = System.currentTimeMillis(),
                timedOut = false,
                numTests = 0,
                numEdgeCaseTests = 0,
                numSimpleCaseTests = 0,
                numSimpleAndEdgeCaseTests = 0,
                numMixedTests = 0,
                numAllGeneratedTests = 0,
                classDesignAnalysisResult = failedCDAResult,
                testSteps = listOf()
            )

    override fun runTests(seed: Long, testRunnerArgs: TestRunnerArgs): TestRunOutput = runTests(seed)
}

private class GeneratorMapBuilder(goalTypes: Collection<Type>, private val random: Random) {
    private var knownGenerators: MutableMap<Type, Lazy<Gen<*>>> = mutableMapOf()
    init {
        defaultGenerators.forEach(this::accept)
        knownGenerators[String::class.java] = lazy { DefaultStringGen(knownGenerators[Char::class.java]!!.value) }
    }

    private val requiredGenerators: Set<Type> = goalTypes.toSet().also { it.forEach(this::request) }

    private fun lazyGenError(type: Type) = AnswerableMisuseException(
        "A generator for type `${type.sourceName}' was requested, but no generator for that type was found."
    )

    private fun lazyArrayError(type: Type) = AnswerableMisuseException(
        "A generator for an array with component type `${type.sourceName}' was requested, but no generator for that type was found."
    )

    fun accept(pair: Pair<Type, Gen<*>?>) = accept(pair.first, pair.second)

    fun accept(type: Type, gen: Gen<*>?) {
        if (gen != null) {
            // kotlin fails to smart cast here even though it says the cast isn't needed
            @Suppress("USELESS_CAST")
            knownGenerators[type] = lazy { gen as Gen<*> }
        }
    }

    private fun request(type: Type) {
        when (type) {
            is Class<*> -> if (type.isArray) {
                request(type.componentType)
                knownGenerators[type] =
                    lazy {
                        DefaultArrayGen(
                            knownGenerators[type.componentType]?.value ?: throw lazyArrayError(type.componentType),
                            type.componentType
                        )
                    }
            }
        }
    }

    fun build(): Map<Type, GenWrapper<*>> = mapOf(*requiredGenerators.map {
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

data class TestRunnerArgs(
    val numTests: Int = 1024,
    val maxOnlyEdgeCaseTests: Int = numTests/16,
    val maxOnlySimpleCaseTests: Int = numTests/16,
    val numSimpleEdgeMixedTests: Int = numTests/16,
    val numAllGeneratedTests: Int = numTests/2,
    val maxComplexity: Int = 100
)
val defaultArgs = TestRunnerArgs()

internal class GenWrapper<T>(val gen: Gen<T>, private val random: Random) {
    operator fun invoke(complexity: Int) = gen.generate(complexity, random)

    fun generate(complexity: Int): T = gen.generate(complexity, random)
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

internal val defaultIntEdgeCases = intArrayOf(0)
internal val defaultIntSimpleCases = intArrayOf(-1, 1)

internal val defaultDoubleGen = object : Gen<Double> {
    override fun generate(complexity: Int, random: Random): Double {
        val denom = random.nextDouble() * (1e10 - 1) + 1
        val num = (random.nextDouble() * 2 * complexity * denom) - complexity * denom
        return num / denom
    }
}

internal val defaultDoubleEdgeCases = doubleArrayOf(0.0)
internal val defaultDoubleSimpleCases = doubleArrayOf(-1.0, 1.0)

internal val defaultFloatGen = object : Gen<Float> {
    override fun generate(complexity: Int, random: Random): Float {
        val denom = random.nextDouble() * (1e10 - 1) + 1
        val num = (random.nextDouble() * 2 * complexity * denom) - complexity * denom
        return (num / denom).toFloat() // if complexity is > 1e38, this stops being uniform
    }
}

internal val defaultFloatEdgeCases = floatArrayOf(0f)
internal val defaultFloatSimpleCases = floatArrayOf(-1f, 1f)

internal val defaultByteGen = object : Gen<Byte> {
    override fun generate(complexity: Int, random: Random): Byte {
        return (random.nextInt(complexity * 2 + 1) - complexity).toByte()
    }
}

internal val defaultByteEdgeCases = byteArrayOf(0)
internal val defaultByteSimpleCases = byteArrayOf(-1, 1)

internal val defaultShortGen = object : Gen<Short> {
    override fun generate(complexity: Int, random: Random): Short {
        return (random.nextInt(complexity * 2 + 1) - complexity).toShort()
    }
}

internal val defaultShortEdgeCases = shortArrayOf(0)
internal val defaultShortSimpleCases = shortArrayOf(-1, 1)

internal val defaultLongGen = object : Gen<Long> {
    // see Random.nextInt(int) algorithm.
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

internal val defaultLongEdgeCases = longArrayOf(0)
internal val defaultLongSimpleCases = longArrayOf(-1, 1)

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

internal val defaultCharEdgeCases = charArrayOf(' ')
internal val defaultCharSimpleCases = charArrayOf('a', 'A', '0')

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

internal val defaultStringEdgeCases = arrayOf(null, "")
internal val defaultStringSimpleCases = arrayOf("a", "A", "0")

internal val defaultBooleanGen = object : Gen<Boolean> {
    override fun generate(complexity: Int, random: Random): Boolean = random.nextInt(2) == 0
}

internal class DefaultArrayGen<T>(private val tGen: Gen<T>, private val tClass: Class<*>) : Gen<Any> {
    override fun generate(complexity: Int, random: Random): Any {
        fun genList(complexity: Int, length: Int): List<T> =
            if (length <= 0) {
                listOf()
            } else {
                listOf(tGen(random.nextInt(complexity + 1), random)) + genList(complexity, length - 1)
            }

        val vals = genList(complexity, random.nextInt(complexity + 1))
        @Suppress("UNCHECKED_CAST")
        return ReflectArray.newInstance(tClass, vals.size).also {
            when (it::class.java) {
                IntArray::class.java -> { vals as List<Int>
                    vals.forEachIndexed { idx, value -> ReflectArray.setInt(it, idx, value) }
                }
                ShortArray::class.java -> { vals as List<Short>
                    vals.forEachIndexed { idx, value -> ReflectArray.setShort(it, idx, value) }
                }
                ByteArray::class.java -> { vals as List<Byte>
                    vals.forEachIndexed { idx, value -> ReflectArray.setByte(it, idx, value) }
                }
                LongArray::class.java -> { vals as List<Long>
                    vals.forEachIndexed { idx, value -> ReflectArray.setLong(it, idx, value) }
                }
                DoubleArray::class.java -> { vals as List<Double>
                    vals.forEachIndexed { idx, value -> ReflectArray.setDouble(it, idx, value) }
                }
                FloatArray::class.java -> { vals as List<Float>
                    vals.forEachIndexed { idx, value -> ReflectArray.setFloat(it, idx, value) }
                }
                CharArray::class.java -> { vals as List<Char>
                    vals.forEachIndexed { idx, value -> ReflectArray.setChar(it, idx, value) }
                }
                BooleanArray::class.java -> { vals as List<Boolean>
                    vals.forEachIndexed { idx, value -> ReflectArray.setBoolean(it, idx, value) }
                }
                else -> vals.forEachIndexed { idx, value -> ReflectArray.set(it, idx, value) }
            }
        }
    }
}

internal val defaultIntArrayEdgeCases = arrayOf(intArrayOf(), null)
internal val defaultIntArraySimpleCases = arrayOf(intArrayOf(0))
internal val defaultByteArrayEdgeCases = arrayOf(byteArrayOf(), null)
internal val defaultByteArraySimpleCases = arrayOf(byteArrayOf(0))
internal val defaultShortArrayEdgeCases = arrayOf(shortArrayOf(), null)
internal val defaultShortArraySimpleCases = arrayOf(shortArrayOf(0))
internal val defaultLongArrayEdgeCases = arrayOf(longArrayOf(), null)
internal val defaultLongArraySimpleCases = arrayOf(longArrayOf(0))
internal val defaultDoubleArrayEdgeCases = arrayOf(doubleArrayOf(), null)
internal val defaultDoubleArraySimpleCases = arrayOf(doubleArrayOf(0.0))
internal val defaultFloatArrayEdgeCases = arrayOf(floatArrayOf(), null)
internal val defaultFloatArraySimpleCases = arrayOf(floatArrayOf(0f))
internal val defaultCharArrayEdgeCases = arrayOf(charArrayOf(), null)
internal val defaultCharArraySimpleCases = arrayOf(charArrayOf(' '))
internal val defaultStringArrayEdgeCases = arrayOf(arrayOf<String>(), null)
internal val defaultStringArraySimpleCases = arrayOf(arrayOf(""))
internal val defaultBooleanArrayEdgeCases = arrayOf(booleanArrayOf(), null)

internal sealed class ArrayWrapper {
    abstract operator fun get(index: Int): Any?
    abstract fun random(random: Random): Any?
    abstract val size: Int

}
internal class AnyArrayWrapper<T>(val arr: Array<T>) : ArrayWrapper() {
    override fun get(index: Int) = arr[index]
    override fun random(random: Random) = arr.random(random.asKotlinRandom())
    override val size: Int
        get() = arr.size

    val sizeWithoutNull: Int
        get() = arr.filter { it != null }.size
}
internal class IntArrayWrapper(private val arr: IntArray) : ArrayWrapper() {
    override fun get(index: Int): Int = arr[index]
    override fun random(random: Random) = arr.random(random.asKotlinRandom())
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as IntArrayWrapper

        if (!arr.contentEquals(other.arr)) return false

        return true
    }

    override fun hashCode(): Int {
        return arr.contentHashCode()
    }

    override val size: Int
        get() = arr.size

}
internal class ByteArrayWrapper(private val arr: ByteArray) : ArrayWrapper() {
    override fun get(index: Int): Byte = arr[index]
    override fun random(random: Random) = arr.random(random.asKotlinRandom())
    override val size: Int
        get() = arr.size
}
internal class ShortArrayWrapper(private val arr: ShortArray) : ArrayWrapper() {
    override fun get(index: Int): Short = arr[index]
    override fun random(random: Random) = arr.random(random.asKotlinRandom())
    override val size: Int
        get() = arr.size
}
internal class LongArrayWrapper(private val arr: LongArray) : ArrayWrapper() {
    override fun get(index: Int): Long = arr[index]
    override fun random(random: Random) = arr.random(random.asKotlinRandom())
    override val size: Int
        get() = arr.size
}
internal class DoubleArrayWrapper(private val arr: DoubleArray) : ArrayWrapper() {
    override fun get(index: Int): Double = arr[index]
    override fun random(random: Random) = arr.random(random.asKotlinRandom())
    override val size: Int
        get() = arr.size
}
internal class FloatArrayWrapper(private val arr: FloatArray) : ArrayWrapper() {
    override fun get(index: Int): Float = arr[index]
    override fun random(random: Random) = arr.random(random.asKotlinRandom())
    override val size: Int
        get() = arr.size
}
internal class CharArrayWrapper(private val arr: CharArray) : ArrayWrapper() {
    override fun get(index: Int): Char = arr[index]
    override fun random(random: Random) = arr.random(random.asKotlinRandom())
    override val size: Int
        get() = arr.size
}
internal class BooleanArrayWrapper(private val arr: BooleanArray) : ArrayWrapper() {
    override fun get(index: Int): Boolean = arr[index]
    override fun random(random: Random) = arr.random(random.asKotlinRandom())
    override val size: Int
        get() = arr.size
}
internal fun ArrayWrapper?.isNullOrEmpty() = this == null || this.size == 0

internal val defaultEdgeCases = mapOf(
    Int::class.java to IntArrayWrapper(defaultIntEdgeCases),
    Byte::class.java to ByteArrayWrapper(defaultByteEdgeCases),
    Short::class.java to ShortArrayWrapper(defaultShortEdgeCases),
    Long::class.java to LongArrayWrapper(defaultLongEdgeCases),
    Double::class.java to DoubleArrayWrapper(defaultDoubleEdgeCases),
    Float::class.java to FloatArrayWrapper(defaultFloatEdgeCases),
    Char::class.java to CharArrayWrapper(defaultCharEdgeCases),
    String::class.java to AnyArrayWrapper(defaultStringEdgeCases),

    IntArray::class.java to AnyArrayWrapper(defaultIntArrayEdgeCases),
    ByteArray::class.java to AnyArrayWrapper(defaultByteArrayEdgeCases),
    ShortArray::class.java to AnyArrayWrapper(defaultShortArrayEdgeCases),
    LongArray::class.java to AnyArrayWrapper(defaultLongArrayEdgeCases),
    DoubleArray::class.java to AnyArrayWrapper(defaultDoubleArrayEdgeCases),
    FloatArray::class.java to AnyArrayWrapper(defaultFloatArrayEdgeCases),
    CharArray::class.java to AnyArrayWrapper(defaultCharArrayEdgeCases),
    Array<String>::class.java to AnyArrayWrapper(defaultStringArrayEdgeCases),
    BooleanArray::class.java to AnyArrayWrapper(defaultBooleanArrayEdgeCases)
)
internal val defaultSimpleCases = mapOf(
    Int::class.java to IntArrayWrapper(defaultIntSimpleCases),
    Byte::class.java to ByteArrayWrapper(defaultByteSimpleCases),
    Short::class.java to ShortArrayWrapper(defaultShortSimpleCases),
    Long::class.java to LongArrayWrapper(defaultLongSimpleCases),
    Double::class.java to DoubleArrayWrapper(defaultDoubleSimpleCases),
    Float::class.java to FloatArrayWrapper(defaultFloatSimpleCases),
    Char::class.java to CharArrayWrapper(defaultCharSimpleCases),
    String::class.java to AnyArrayWrapper(defaultStringSimpleCases),

    IntArray::class.java to AnyArrayWrapper(defaultIntArraySimpleCases),
    ByteArray::class.java to AnyArrayWrapper(defaultByteArraySimpleCases),
    ShortArray::class.java to AnyArrayWrapper(defaultShortArraySimpleCases),
    LongArray::class.java to AnyArrayWrapper(defaultLongArraySimpleCases),
    DoubleArray::class.java to AnyArrayWrapper(defaultDoubleArraySimpleCases),
    FloatArray::class.java to AnyArrayWrapper(defaultFloatArraySimpleCases),
    CharArray::class.java to AnyArrayWrapper(defaultCharArraySimpleCases),
    Array<String>::class.java to AnyArrayWrapper(defaultStringArraySimpleCases)
)


internal class DefaultListGen<T>(private val tGen: Gen<T>) : Gen<List<T>> {
    override fun generate(complexity: Int, random: Random): List<T> {
        fun genList(complexity: Int, length: Int): List<T> =
            if (length <= 0) {
                listOf()
            } else {
                listOf(tGen(random.nextInt(complexity + 1), random)) + genList(complexity, length - 1)
            }
        return genList(complexity, random.nextInt(complexity + 1))
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

enum class Behavior { RETURNED, THREW, VERIFY_ONLY }

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

data class TestRunOutput(
    val seed: Long,
    val testedClass: Class<*>,
    val solutionName: String,
    val startTime: Long,
    val endTime: Long,
    val timedOut: Boolean,
    val numTests: Int,
    val numEdgeCaseTests: Int,
    val numSimpleCaseTests: Int,
    val numSimpleAndEdgeCaseTests: Int,
    val numMixedTests: Int,
    val numAllGeneratedTests: Int,
    val classDesignAnalysisResult: List<AnalysisOutput>,
    val testSteps: List<TestStep>
) : DefaultSerializable {
    override fun toJson() = defaultToJson()
}