package edu.illinois.cs.cs125.answerable

import edu.illinois.cs.cs125.answerable.TestGenerator.ReceiverGenStrategy.*
import edu.illinois.cs.cs125.answerable.api.*
import edu.illinois.cs.cs125.answerable.typeManagement.*
import org.junit.jupiter.api.Assertions.*
import org.opentest4j.AssertionFailedError
import java.util.*
import kotlin.math.min
import java.lang.Character.UnicodeBlock.*
import java.lang.IllegalStateException
import java.lang.reflect.*
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.lang.reflect.Array as ReflectArray

/**
 * A generator for testing runs.
 *
 * Each [TestGenerator] is bound to a particular [solutionName] on a particular [referenceClass]. Whenever a
 * class is 'submitted' to the [TestGenerator], the [TestGenerator] will produce a [TestRunner] which can execute
 * a random test suite given a seed. Given the same seed, the [TestRunner] will always run the same test cases.
 *
 * You can also provide [TestRunnerArgs] which will be used as defaults for [TestRunner]s produced by this
 * [TestGenerator]. [TestRunnerArgs] can also be supplied when testing is initiated. If none are provided,
 * Answerable will use a set of [defaultArgs].
 *
 * @constructor Creates a [TestGenerator] for the [referenceClass] @[Solution] method named [solutionName],
 * which creates [TestRunner]s which default to using [testRunnerArgs]. If [referenceClass] was loaded dynamically,
 * a [BytecodeProvider] must be specified that can determine its bytecode. If the reference or solution class
 * refers to a class that was loaded dynamically, the [commonClassloader] for that class must be provided.
 */
class TestGenerator(
    val referenceClass: Class<*>,
    val solutionName: String = "",
    private val testRunnerArgs: TestRunnerArgs = defaultArgs,
    private val bytecodeProvider: BytecodeProvider? = null,
    private val commonClassloader: ClassLoader? = null
) {
    /**
     * A secondary constructor which uses Answerable's [defaultArgs] and no custom bytecode provider.
     */
    constructor(referenceClass: Class<*>, solutionName: String) : this(referenceClass, solutionName, defaultArgs)
    init {
        verifyStaticSignatures(referenceClass)
    }

    // "Usable" members are from the opened (un-final-ified) mirror of the original reference class.
    // The original members are used for certain checks so a nice class name can be displayed.

    internal val typePool = TypePool(bytecodeProvider, commonClassloader ?: javaClass.classLoader)
    internal val usableReferenceClass = mkOpenMirrorClass(referenceClass, typePool, "openref_")
    internal val usableReferenceMethod = usableReferenceClass.getReferenceSolutionMethod(solutionName)

    private val referenceMethod: Method? = referenceClass.getReferenceSolutionMethod(solutionName)
    internal val enabledNames: Array<String> =
        referenceMethod?.getAnnotation(Solution::class.java)?.enabled ?: arrayOf()

    internal val usablePrecondition: Method? = usableReferenceClass.getPrecondition(solutionName)
    private val customVerifier: Method? = referenceClass.getCustomVerifier(solutionName)
    internal val usableCustomVerifier: Method? = usableReferenceClass.getCustomVerifier(solutionName)

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
    internal val atNextMethod: Method? = usableReferenceClass.getAtNext(enabledNames)

    internal val isStatic = referenceMethod?.let { Modifier.isStatic(it.modifiers) } ?: false
    internal val paramTypes: Array<Type> = usableReferenceMethod?.genericParameterTypes ?: arrayOf()
    internal val paramTypesWithReceiver: Array<Type> = arrayOf(usableReferenceClass, *paramTypes)

    internal val random: Random = Random(0)
    internal val generators: Map<Type, GenWrapper<*>> = buildGeneratorMap(random)
    internal val edgeCases: Map<Type, ArrayWrapper?> = getEdgeCases(usableReferenceClass, paramTypesWithReceiver)
    internal val simpleCases: Map<Type, ArrayWrapper?> = getSimpleCases(usableReferenceClass, paramTypesWithReceiver)

    internal val timeout = referenceMethod?.getAnnotation(Timeout::class.java)?.timeout
        ?: (customVerifier?.getAnnotation(Timeout::class.java)?.timeout ?: 0)

    internal enum class ReceiverGenStrategy { GENERATOR, NEXT, NONE }
    internal val receiverGenStrategy: ReceiverGenStrategy = when {
        atNextMethod != null                  -> NEXT
        usableReferenceClass in generators.keys -> GENERATOR
        isStatic                              -> NONE
        else -> throw AnswerableMisuseException("The reference solution must provide either an @Generator or an @Next method if @Solution is not static.")
    }

    init {
        verifySafety()
    }

    internal fun buildGeneratorMap(random: Random, submittedClassGenerator: Method? = null): Map<Type, GenWrapper<*>> {
        val types = paramTypes.toSet().let {
            if (!isStatic && atNextMethod == null) {
                it + usableReferenceClass
            } else it
        }

        val generatorMapBuilder = GeneratorMapBuilder(types, random)

        val userGens = usableReferenceClass.getEnabledGenerators(enabledNames).map {
            return@map if (it.returnType == usableReferenceClass && submittedClassGenerator != null) {
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

    private fun verifySafety() {
        verifyMemberAccess(referenceClass, typePool)

        val dryRun = { loadSubmission(
                mkOpenMirrorClass(referenceClass, typePool, "dryrunopenref_"),
                bytecodeProvider = typePool.getLoader(),
                runClassDesign = false).runTestsUnsecured(0x0403) }
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
            if (it is ExecutedTestStep) {
                if (!it.succeeded) {
                    throw AnswerableVerificationException(
                        "Testing reference against itself failed on inputs: ${Arrays.deepToString(
                            it.refOutput.args
                        )}"
                    ).initCause(it.assertErr)
                }
            }
        }
    }

    /**
     * Load a submission class to the problem represented by this [TestGenerator].
     *
     * The submission class will be run through Class Design Analysis against the reference solution.
     * The results of class design analysis will be included in the output of every test run by the [TestRunner] returned.
     * If class design analysis fails, the returned [TestRunner] will never execute any tests, as doing so
     * would be unsafe and cause nasty errors.
     *
     * @param submissionClass the class to be tested against the reference
     * @param testRunnerArgs the arguments that the [TestRunner] returned should default to.
     * @param bytecodeProvider provider of bytecode for the submission class(es), or null if not loaded dynamically
     */
    fun loadSubmission(
            submissionClass: Class<*>,
            testRunnerArgs: TestRunnerArgs = this.testRunnerArgs,
            bytecodeProvider: BytecodeProvider? = null
    ): TestRunner {
        return loadSubmission(submissionClass, testRunnerArgs, bytecodeProvider, true)
    }

    private fun loadSubmission(
        submissionClass: Class<*>,
        testRunnerArgs: TestRunnerArgs = this.testRunnerArgs,
        bytecodeProvider: BytecodeProvider? = null,
        runClassDesign: Boolean = true
    ): TestRunner {
        val cda = ClassDesignAnalysis(solutionName, referenceClass, submissionClass).runSuite()
        val cdaPassed = cda.all { ao -> ao.result is Matched }

        // when testing the reference against itself, we fully expect that public methods and fields
        // won't match, particularly @Generators, @Verifies, @EdgeCases etc.
        // We can't just exclude them from class design analysis in the submission because then real submissions
        // could subvert the system. On the other hand, it's impossible for the reference class to be unsafe against itself.
        // So we simply allow testing to continue if the reference and submission classes are the same class.
        return if (cdaPassed || !runClassDesign) {
            PassedClassDesignRunner(this, submissionClass, cda, testRunnerArgs, bytecodeProvider)
        } else {
            FailedClassDesignTestRunner(referenceClass, solutionName, submissionClass, cda)
        }
    }

}

/**
 * Represents a class that can execute tests.
 */
interface TestRunner {
    fun runTests(seed: Long, environment: TestEnvironment, testRunnerArgs: TestRunnerArgs): TestRunOutput
    fun runTests(seed: Long, environment: TestEnvironment): TestRunOutput
}

fun TestRunner.runTestsUnsecured(seed: Long, testRunnerArgs: TestRunnerArgs = defaultArgs)
        = this.runTests(seed, defaultEnvironment, testRunnerArgs)

/**
 * The primary [TestRunner] subclass which tests classes that have passed Class Design Analysis.
 *
 * The only publicly-exposed way to create a [PassedClassDesignRunner] is to invoke
 * [TestGenerator.loadSubmission] on an existing [TestGenerator].
 */
class PassedClassDesignRunner internal constructor(
        private val testGenerator: TestGenerator,
        private val submissionClass: Class<*>,
        private val cachedClassDesignAnalysisResult: List<AnalysisOutput> = listOf(),
        private val testRunnerArgs: TestRunnerArgs,
        private val bytecodeProvider: BytecodeProvider?
) : TestRunner {

    internal constructor(
            testGenerator: TestGenerator, submissionClass: Class<*>, cdaResult: List<AnalysisOutput> = listOf(), testRunnerArgs: TestRunnerArgs = defaultArgs
    ) : this(testGenerator, submissionClass, cdaResult, testRunnerArgs, null)

    internal constructor(
            referenceClass: Class<*>, submissionClass: Class<*>, cdaResult: List<AnalysisOutput> = listOf(), testRunnerArgs: TestRunnerArgs = defaultArgs
    ) : this(TestGenerator(referenceClass), submissionClass, cdaResult, testRunnerArgs)

    /**
     * [TestRunner.runTests] override which accepts [TestRunnerArgs]. Executes a test suite.
     *
     * If the method under test has a timeout, [runTests] will run as many tests as it can before the timeout
     * is reached, and record the results.
     *
     * When called with the same [seed], [runTests] will always produce the same result.
     */
    override fun runTests(seed: Long, environment: TestEnvironment, testRunnerArgs: TestRunnerArgs): TestRunOutput {
        val submissionTypePool = TypePool(bytecodeProvider, submissionClass.classLoader)
        val untrustedSubMirror = mkOpenMirrorClass(submissionClass, submissionTypePool, "opensub_")
        val loader = environment.sandbox.transformLoader(submissionTypePool.getLoader())
        val sandboxedSubMirror = Class.forName(untrustedSubMirror.name, false, loader.getLoader())
        val worker = TestRunWorker(testGenerator, sandboxedSubMirror, environment, loader)

        val testSteps = mutableListOf<TestStep>()
        val testingBlockCounts = TestingBlockCounts()
        val startTime = System.currentTimeMillis()
        val timedOut = !environment.sandbox.run(if (testGenerator.timeout == 0L) null else testGenerator.timeout, Runnable {
            worker.runTests(seed, testRunnerArgs, testSteps, testingBlockCounts)
        })
        val endTime = System.currentTimeMillis()

        return TestRunOutput(
                seed = seed,
                referenceClass = testGenerator.referenceClass,
                testedClass = submissionClass,
                solutionName = testGenerator.solutionName,
                startTime = startTime,
                endTime = endTime,
                timedOut = timedOut,
                numDiscardedTests = testingBlockCounts.discardedTests,
                numTests = testingBlockCounts.numTests,
                numEdgeCaseTests = testingBlockCounts.edgeTests,
                numSimpleCaseTests = testingBlockCounts.simpleTests,
                numSimpleAndEdgeCaseTests = testingBlockCounts.simpleEdgeMixedTests,
                numMixedTests = testingBlockCounts.generatedMixedTests,
                numAllGeneratedTests = testingBlockCounts.allGeneratedTests,
                classDesignAnalysisResult = cachedClassDesignAnalysisResult,
                testSteps = testSteps
        )
    }

    /**
     * [TestRunner.runTests] overload which uses the [TestRunnerArgs] that this [PassedClassDesignRunner] was constructed with.
     */
    override fun runTests(seed: Long, environment: TestEnvironment) = runTests(seed, environment, this.testRunnerArgs) // to expose the overload to Java
}

class TestRunWorker internal constructor(
    testGenerator: TestGenerator,
    private val usableSubmissionClass: Class<*>,
    private val environment: TestEnvironment,
    private val bytecodeProvider: BytecodeProvider?
) {
    private val usableReferenceClass = testGenerator.usableReferenceClass
    private val usableReferenceMethod = testGenerator.usableReferenceMethod
    private val usableCustomVerifier = testGenerator.usableCustomVerifier

    private val submissionTypePool = TypePool(bytecodeProvider, usableSubmissionClass.classLoader)
    private val adapterTypePool = TypePool(testGenerator.typePool, submissionTypePool)
    private val usableSubmissionMethod = usableSubmissionClass.findSolutionAttemptMethod(usableReferenceMethod)

    private val paramTypes = testGenerator.paramTypes
    private val paramTypesWithReceiver = testGenerator.paramTypesWithReceiver

    private val precondition = testGenerator.usablePrecondition

    private val testRunnerRandom = Random(0)
    private val randomForReference = testGenerator.random
    private val randomForSubmission = Random(0)

    private val mirrorToStudentClass = mkGeneratorMirrorClass(usableReferenceClass, usableSubmissionClass, adapterTypePool, "genmirror_")

    private val referenceEdgeCases = testGenerator.edgeCases
    private val referenceSimpleCases = testGenerator.simpleCases
    private val submissionEdgeCases: Map<Type, ArrayWrapper?> = referenceEdgeCases
        // replace reference class cases with mirrored cases
        // the idea is that each map takes `paramTypes` to the correct generator/cases
        .toMutableMap().apply {
            replace(
                usableReferenceClass,
                mirrorToStudentClass.getEnabledEdgeCases(testGenerator.enabledNames)[usableSubmissionClass]
            )
        }
    private val submissionSimpleCases: Map<Type, ArrayWrapper?> = referenceSimpleCases
        // replace reference class cases with mirrored cases
        .toMutableMap().apply {
            replace(
                usableReferenceClass,
                mirrorToStudentClass.getEnabledSimpleCases(testGenerator.enabledNames)[usableSubmissionClass]
            )
        }

    private val numEdgeCombinations = calculateNumCases(referenceEdgeCases)
    private val numSimpleCombinations = calculateNumCases(referenceSimpleCases)

    private val referenceGens = testGenerator.generators
    private val submissionGens = mirrorToStudentClass
        .getEnabledGenerators(testGenerator.enabledNames)
        .find { it.returnType == usableSubmissionClass }
        .let { testGenerator.buildGeneratorMap(randomForSubmission, it) }
    private val referenceAtNext = testGenerator.atNextMethod
    private val submissionAtNext = mirrorToStudentClass.getAtNext(testGenerator.enabledNames)

    private val receiverGenStrategy = testGenerator.receiverGenStrategy
    private val capturePrint = usableReferenceMethod?.getAnnotation(Solution::class.java)?.prints ?: false
    private val isStatic = testGenerator.isStatic
    private val timeout = testGenerator.timeout

    private fun calculateNumCases(cases: Map<Type, ArrayWrapper?>): Int =
        paramTypesWithReceiver.foldIndexed(1) { idx, acc, type ->
            cases[type]?.let { cases: ArrayWrapper ->
                (if (idx == 0) ((cases.array as? Array<*>)?.filterNotNull()?.size ?: 1) else cases.size).let {
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
                val typeCasesArr = typeCases.array as? Array<*> ?: throw IllegalStateException("Answerable thinks a receiver type is primitive. Please report a bug.")
                val typeCasesLst = typeCasesArr.filterNotNull() // receivers can't be null

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
            subProxy = mkProxy(usableReferenceClass, usableSubmissionClass, subReceiver!!, adapterTypePool)
        }

        return test(iteration, refReceiver, subReceiver, subProxy, refMethodArgs, subMethodArgs)
    }

    private fun mkRefReceiver(iteration: Int, complexity: Int, prevRefReceiver: Any?): Any? =
        when (receiverGenStrategy) {
            NONE -> null
            GENERATOR -> referenceGens[usableReferenceClass]?.generate(complexity)
            NEXT -> referenceAtNext?.invoke(null, prevRefReceiver, iteration, randomForReference)
        }

    private fun mkSubReceiver(iteration: Int, complexity: Int, prevSubReceiver: Any?): Any? =
        when (receiverGenStrategy) {
            NONE -> null
            GENERATOR -> submissionGens[usableReferenceClass]?.generate(complexity)
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
            var behavior: Behavior? = null

            var threw: Throwable? = null
            var outText: String? = null
            var errText: String? = null
            var output: Any? = null
            if (method != null) {
                val toRun = Runnable {
                    try {
                        output = method(receiver, *args)
                        behavior = Behavior.RETURNED
                    } catch (e: Throwable) {
                        threw = e.cause ?: e
                        behavior = Behavior.THREW
                    }
                }
                if (capturePrint) {
                    environment.outputCapturer.runCapturingOutput(toRun)
                    outText = environment.outputCapturer.getStandardOut()
                    errText = environment.outputCapturer.getStandardErr()
                } else {
                    toRun.run()
                }
            } else {
                behavior = Behavior.VERIFY_ONLY
            }
            return TestOutput(
                typeOfBehavior = behavior!!,
                receiver = refCompatibleReceiver,
                args = args,
                output = output,
                threw = threw,
                stdOut = outText,
                stdErr = errText
            )
        }

        val refBehavior = runOne(refReceiver, refReceiver, usableReferenceMethod, refArgs)
        val subBehavior = runOne(subReceiver, subProxy, usableSubmissionMethod, subArgs)

        var assertErr: Throwable? = null
        try {
            if (usableCustomVerifier == null) {
                assertEquals(refBehavior.threw?.javaClass, subBehavior.threw?.javaClass)
                assertEquals(refBehavior.output, subBehavior.output)
                assertEquals(refBehavior.stdOut, subBehavior.stdOut)
                assertEquals(refBehavior.stdErr, subBehavior.stdErr)
            } else {
                if (subProxy != null) {
                    usableSubmissionClass.getPublicFields().forEach {
                        usableReferenceClass.getField(it.name).set(subProxy, it.get(subReceiver))
                    }
                }
                usableCustomVerifier.invoke(null, refBehavior, subBehavior)
            }
        } catch (ite: InvocationTargetException) {
            assertErr = ite.cause
        } catch (e: AssertionFailedError) {
            assertErr = e
        }

        return ExecutedTestStep(
            iteration = iteration,
            refReceiver = refReceiver,
            subReceiver = subReceiver,
            succeeded = assertErr == null,
            refOutput = refBehavior,
            subOutput = subBehavior,
            assertErr = assertErr
        )
    }

    fun runTests(seed: Long, testRunnerArgs: TestRunnerArgs, testStepList: MutableList<TestStep>, testingBlockCounts: TestingBlockCounts) {
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

        val simpleCaseUpperBound = numEdgeCaseTests + numSimpleCaseTests
        val simpleEdgeMixedUpperBound = simpleCaseUpperBound + numSimpleEdgeMixedTests

        val numGeneratedMixedTests: Int
                by lazy { numTests -
                        numAllGeneratedTests -
                        testingBlockCounts.let { it.edgeTests + it.simpleTests + it.simpleEdgeMixedTests }
                }

        setOf(randomForReference, randomForSubmission, testRunnerRandom).forEach { it.setSeed(seed) }

        // Store reference class static field values so that the next run against this solution doesn't break
        val refStaticFieldValues = usableReferenceClass.declaredFields.filter { Modifier.isStatic(it.modifiers) }.map {
            it.isAccessible = true
            it to it.get(null)
        }

        var refReceiver: Any? = null
        var subReceiver: Any? = null

        var block: Int
        var generatedMixedIdx = 0
        var allGeneratedIdx = 0

        var i = 0
        while (testingBlockCounts.numTests < testRunnerArgs.numTests) {
            val refMethodArgs: Array<Any?>
            val subMethodArgs: Array<Any?>
            when {
                i in 1 .. numEdgeCaseTests -> {
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
                i in (numEdgeCaseTests + 1) .. simpleCaseUpperBound -> {
                    block = 1

                    val idxInSegment = i - numEdgeCaseTests - 1
                    val idx = if (simpleExhaustive) idxInSegment else testRunnerRandom.nextInt(numSimpleCombinations)

                    val refCase = calculateCase(idx, numSimpleCombinations, referenceSimpleCases, referenceGens)
                    val subCase = calculateCase(idx, numSimpleCombinations, submissionSimpleCases, submissionGens)

                    refMethodArgs = refCase.slice(1..refCase.indices.last).toTypedArray()
                    subMethodArgs = subCase.slice(1..subCase.indices.last).toTypedArray()

                    refReceiver = if (refCase[0] != null) refCase[0] else mkRefReceiver(i, 0, refReceiver)
                    subReceiver = if (subCase[0] != null) subCase[0] else mkSubReceiver(i, 0, subReceiver)

                }
                i in (simpleCaseUpperBound + 1) .. simpleEdgeMixedUpperBound -> {
                    block = 2

                    refReceiver = mkRefReceiver(i, 2, refReceiver)
                    subReceiver = mkSubReceiver(i, 2, subReceiver)

                    refMethodArgs = mkSimpleEdgeMixedCase(referenceEdgeCases, referenceSimpleCases, referenceGens, randomForReference)
                    subMethodArgs = mkSimpleEdgeMixedCase(submissionEdgeCases, submissionSimpleCases, submissionGens, randomForSubmission)
                }
                testingBlockCounts.allGeneratedTests < numAllGeneratedTests -> {
                    block = 3

                    val comp = min((testRunnerArgs.maxComplexity * allGeneratedIdx) / numAllGeneratedTests, testRunnerArgs.maxComplexity)

                    refReceiver = mkRefReceiver(i, comp, refReceiver)
                    subReceiver = mkSubReceiver(i, comp, subReceiver)

                    refMethodArgs = paramTypes.map { referenceGens[it]?.generate(comp) }.toTypedArray()
                    subMethodArgs = paramTypes.map { submissionGens[it]?.generate(comp) }.toTypedArray()

                    allGeneratedIdx++
                }
                testingBlockCounts.numTests < numTests -> {
                    block = 4

                    val comp = min((testRunnerArgs.maxComplexity * generatedMixedIdx) / numGeneratedMixedTests, testRunnerArgs.maxComplexity)

                    refReceiver = mkRefReceiver(i, comp, refReceiver)
                    subReceiver = mkSubReceiver(i, comp, subReceiver)

                    refMethodArgs = mkGeneratedMixedCase(referenceEdgeCases, referenceSimpleCases, referenceGens, comp, randomForReference)
                    subMethodArgs = mkGeneratedMixedCase(submissionEdgeCases, submissionSimpleCases, submissionGens, comp, randomForSubmission)

                    generatedMixedIdx++
                }

                else ->
                    throw IllegalStateException(
                            "Answerable somehow lost proper track of test block counts. Please report a bug."
                    )
            }

            val preconditionMet: Boolean = (precondition?.invoke(refReceiver, *refMethodArgs) ?: true) as Boolean

            val result: TestStep
            if (preconditionMet) {
                result = testWith(i, refReceiver, subReceiver, refMethodArgs, subMethodArgs)
                when (block) {
                    0 -> testingBlockCounts.edgeTests++
                    1 -> testingBlockCounts.simpleTests++
                    2 -> testingBlockCounts.simpleEdgeMixedTests++
                    3 -> testingBlockCounts.allGeneratedTests++
                    4 -> testingBlockCounts.generatedMixedTests++
                }
            } else {
                result = DiscardedTestStep(i, refReceiver, refMethodArgs)
                testingBlockCounts.discardedTests++
            }
            testStepList.add(result)

            if (testingBlockCounts.discardedTests >= testRunnerArgs.maxDiscards) break
            i++
        }

        // Restore reference class static field values
        refStaticFieldValues.forEach { (field, value) -> field.set(null, value) }
    }

}

/**
 * The secondary [TestRunner] subclass representing a [submissionClass] which failed Class Design Analysis
 * against the [referenceClass].
 *
 * [runTests] will always execute 0 tests and produce an empty [TestRunOutput.testSteps].
 * The class design analysis results will be contained in the output.
 */
class FailedClassDesignTestRunner(
    private val referenceClass: Class<*>,
    private val solutionName: String,
    private val submissionClass: Class<*>,
    private val failedCDAResult: List<AnalysisOutput>
) : TestRunner {
    override fun runTests(seed: Long, environment: TestEnvironment): TestRunOutput =
            TestRunOutput(
                seed = seed,
                referenceClass = referenceClass,
                testedClass = submissionClass,
                solutionName = solutionName,
                startTime = System.currentTimeMillis(),
                endTime = System.currentTimeMillis(),
                timedOut = false,
                numDiscardedTests = 0,
                numTests = 0,
                numEdgeCaseTests = 0,
                numSimpleCaseTests = 0,
                numSimpleAndEdgeCaseTests = 0,
                numMixedTests = 0,
                numAllGeneratedTests = 0,
                classDesignAnalysisResult = failedCDAResult,
                testSteps = listOf()
            )

    override fun runTests(seed: Long, environment: TestEnvironment, testRunnerArgs: TestRunnerArgs): TestRunOutput = runTests(seed, environment)
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
            val wrapper = ArrayWrapper(it)
            vals.forEachIndexed { idx, value -> wrapper[idx] = value }
        }
    }
}

internal val defaultIntArraySimpleCases = arrayOf(intArrayOf(0))
internal val defaultByteArraySimpleCases = arrayOf(byteArrayOf(0))
internal val defaultShortArraySimpleCases = arrayOf(shortArrayOf(0))
internal val defaultLongArraySimpleCases = arrayOf(longArrayOf(0))
internal val defaultDoubleArraySimpleCases = arrayOf(doubleArrayOf(0.0))
internal val defaultFloatArraySimpleCases = arrayOf(floatArrayOf(0f))
internal val defaultCharArraySimpleCases = arrayOf(charArrayOf(' '))
internal val defaultStringArraySimpleCases = arrayOf(arrayOf(""))

internal class ArrayWrapper(val array: Any) {
    val size = ReflectArray.getLength(array)
    operator fun get(index: Int): Any? {
        return ReflectArray.get(array, index)
    }
    operator fun set(index: Int, value: Any?) {
        ReflectArray.set(array, index, value)
    }
    fun random(random: Random): Any? {
        return get(random.nextInt(size))
    }
}
internal fun ArrayWrapper?.isNullOrEmpty() = this == null || this.size == 0

internal val defaultEdgeCases = mapOf(
    Int::class.java to ArrayWrapper(defaultIntEdgeCases),
    Byte::class.java to ArrayWrapper(defaultByteEdgeCases),
    Short::class.java to ArrayWrapper(defaultShortEdgeCases),
    Long::class.java to ArrayWrapper(defaultLongEdgeCases),
    Double::class.java to ArrayWrapper(defaultDoubleEdgeCases),
    Float::class.java to ArrayWrapper(defaultFloatEdgeCases),
    Char::class.java to ArrayWrapper(defaultCharEdgeCases),
    String::class.java to ArrayWrapper(defaultStringEdgeCases),
    Boolean::class.java to ArrayWrapper(booleanArrayOf())
).let {
    it + it.map { (clazz, _) ->
        val emptyArray = ReflectArray.newInstance(clazz, 0)
        emptyArray.javaClass to ArrayWrapper(arrayOf(emptyArray, null))
    }
}.toMap()
internal val defaultSimpleCases = mapOf(
    Int::class.java to ArrayWrapper(defaultIntSimpleCases),
    Byte::class.java to ArrayWrapper(defaultByteSimpleCases),
    Short::class.java to ArrayWrapper(defaultShortSimpleCases),
    Long::class.java to ArrayWrapper(defaultLongSimpleCases),
    Double::class.java to ArrayWrapper(defaultDoubleSimpleCases),
    Float::class.java to ArrayWrapper(defaultFloatSimpleCases),
    Char::class.java to ArrayWrapper(defaultCharSimpleCases),
    String::class.java to ArrayWrapper(defaultStringSimpleCases),

    IntArray::class.java to ArrayWrapper(defaultIntArraySimpleCases),
    ByteArray::class.java to ArrayWrapper(defaultByteArraySimpleCases),
    ShortArray::class.java to ArrayWrapper(defaultShortArraySimpleCases),
    LongArray::class.java to ArrayWrapper(defaultLongArraySimpleCases),
    DoubleArray::class.java to ArrayWrapper(defaultDoubleArraySimpleCases),
    FloatArray::class.java to ArrayWrapper(defaultFloatArraySimpleCases),
    CharArray::class.java to ArrayWrapper(defaultCharArraySimpleCases),
    Array<String>::class.java to ArrayWrapper(defaultStringArraySimpleCases)
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

internal fun verifyStaticSignatures(referenceClass: Class<*>) {
    val allMethods = referenceClass.declaredMethods

    verifyGenerators(allMethods)
    verifyNexts(referenceClass, allMethods)
    verifyVerifiers(allMethods)
    verifyPreconditions(allMethods)
    verifyCaseMethods(allMethods)
    verifyCaseFields(referenceClass, referenceClass.declaredFields)
}

private val generatorPTypes = arrayOf(Int::class.java, java.util.Random::class.java)
private fun verifyGenerators(methods: Array<Method>) {
    val generators = methods.filter { it.isAnnotationPresent(Generator::class.java) }

    generators.forEach { method ->
        if (!Modifier.isStatic(method.modifiers)) {
            throw AnswerableMisuseException("""
                @Generator methods must be static.
                While verifying generator method `${MethodData(method)}'.
            """.trimIndent())
        }

        if (!(method.parameterTypes contentEquals generatorPTypes)) {
            throw AnswerableMisuseException("""
                @Generator methods must take parameter types [int, Random].
                While verifying @Generator method `${MethodData(method)}'.
            """.trimIndent())
        }
    }
}

private fun verifyNexts(clazz: Class<*>, methods: Array<Method>) {
    val nexts = methods.filter { it.isAnnotationPresent(Next::class.java) }

    nexts.forEach { method ->
        if (!Modifier.isStatic(method.modifiers)) {
            throw AnswerableMisuseException("""
                @Next methods must be static.
                While verifying @Next method `${MethodData(method)}'.
            """.trimIndent())
        }

        if (!(method.parameterTypes contentEquals arrayOf(clazz, Int::class.java, java.util.Random::class.java))) {
            throw AnswerableMisuseException("""
                @Next method must take parameter types [${clazz.sourceName}, int, Random].
                While verifying @Next method `${MethodData(method)}'.
            """.trimIndent())
        }
    }
}

private val verifyPTypes = arrayOf(TestOutput::class.java, TestOutput::class.java)
private fun verifyVerifiers(methods: Array<Method>) {
    val verifiers = methods.filter { it.isAnnotationPresent(Verify::class.java) }

    verifiers.forEach { method ->
        if (!Modifier.isStatic(method.modifiers)) {
            throw AnswerableMisuseException("""
                @Verify methods must be static.
                While verifying @Verify method `${MethodData(method)}'.
            """.trimIndent())
        }

        if (!(method.parameterTypes contentEquals verifyPTypes)) {
            throw AnswerableMisuseException("""
                @Verify methods must take parameter types [TestOutput, TestOutput].
                While verifying @Verify method `${MethodData(method)}'.
            """.trimIndent())
        }

        if (method.returnType != Void.TYPE) {
            throw AnswerableMisuseException("""
                @Verify methods should be void. Throw an exception if verification fails.
                While verifying @Verify method `${MethodData(method)}'.
            """.trimIndent())
        }
    }
}

private fun verifyPreconditions(methods: Array<Method>) {
    val preconditions = methods.filter { it.isAnnotationPresent(Precondition::class.java) }

    preconditions.forEach { method ->
        if (method.returnType != Boolean::class.java) {
            throw AnswerableMisuseException("""
                @Precondition methods must return a boolean.
                While verifying @Precondition method `${MethodData(method)}'.
            """.trimIndent())
        }

        val solution = methods.find {
            it.getAnnotation(Solution::class.java)?.name?.equals(method.getAnnotation(Precondition::class.java)?.name)
                ?: false } ?: return@forEach // nothing to compare to

        if (Modifier.isStatic(solution.modifiers) && !Modifier.isStatic(method.modifiers)) {
            throw AnswerableMisuseException("""
                @Precondition methods must be static if the corresponding @Solution is static.
                While verifying @Precondition method `${MethodData(method)}'.
            """.trimIndent())
        }

        if (!solution.genericParameterTypes!!.contentEquals(method.genericParameterTypes)) {
            throw AnswerableMisuseException("""
                @Precondition methods must have the same parameter types as the corresponding @Solution.
                While verifying @Precondition method `${MethodData(method)}'.
            """.trimIndent())
        }
    }
}

private val caseAnnotations = setOf(EdgeCase::class.java, SimpleCase::class.java)
private fun verifyCaseMethods(methods: Array<Method>) {
    val cases = methods.filter { method -> caseAnnotations.any { method.isAnnotationPresent(it) } }

    cases.forEach { method ->
        val caseString = if (method.isAnnotationPresent(EdgeCase::class.java)) "@EdgeCase" else "@SimpleCase"

        if (!Modifier.isStatic(method.modifiers)) {
            throw AnswerableMisuseException("""
                $caseString methods must be static.
                While verifying $caseString method `${MethodData(method)}'.
            """.trimIndent())
        }

        if (method.parameterTypes.isNotEmpty()) {
            throw AnswerableMisuseException("""
                $caseString methods must not take any parameters.
                While verifying $caseString method `${MethodData(method)}'.
            """.trimIndent())
        }

        if (!method.returnType.isArray) {
            throw AnswerableMisuseException("""
                $caseString methods must return an array.
                While verifying $caseString method `${MethodData(method)}'.
            """.trimIndent())
        }
    }
}

private fun verifyCaseFields(clazz: Class<*>, fields: Array<Field>) {
    val cases = fields.filter { field -> caseAnnotations.any { field.isAnnotationPresent(it) } }

    cases.forEach { field ->
        val caseString = if (field.isAnnotationPresent(EdgeCase::class.java)) "@EdgeCase" else "@SimpleCase"

        if (!Modifier.isStatic(field.modifiers)) {
            throw AnswerableMisuseException("""
                $caseString fields must be static.
                While verifying $caseString field `$field'.
            """.trimIndent())
        }

        if (!field.type.isArray) {
            throw AnswerableMisuseException("""
                $caseString fields must store an array.
                While verifying $caseString field `$field'.
            """.trimIndent())
        }

        if (field.type == clazz) {
            throw AnswerableMisuseException("""
                $caseString cases for the reference class must be represented by a function.
                While verifying $caseString field `$field'.
            """.trimIndent())
        }
    }
}

/**
 * The types of behaviors that methods under test can have.
 */
enum class Behavior { RETURNED, THREW, VERIFY_ONLY }

/**
 * Represents a single iteration of the main testing loop.
 */
abstract class TestStep(
    /** The number of the test represented by this [TestStep]. */
    val testNumber: Int,
    /** Whether or not this test case was discarded. */
    val wasDiscarded: Boolean
) : DefaultSerializable

/**
 * Represents a test case that was executed.
 */
class ExecutedTestStep(
    iteration: Int,
    /** The receiver object passed to the reference. */
    val refReceiver: Any?,
    /** The receiver object passed to the submission. */
    val subReceiver: Any?,
    /** Whether or not the test case succeeded. */
    val succeeded: Boolean,
    /** The return value of the reference solution. */
    val refOutput: TestOutput<Any?>,
    /** The return value of the submission. */
    val subOutput: TestOutput<Any?>,
    /** The assertion error thrown, if any, by the verifier. */
    val assertErr: Throwable?
) : TestStep(iteration, false) {
    override fun toJson() = defaultToJson()
}

/**
 * Represents a discarded test case.
 */
class DiscardedTestStep(
    iteration: Int,
    /** The receiver object that was passed to the precondition. */
    val receiver: Any?,
    /** The other arguments that were passed to the precondition. */
    val args: Array<Any?>
) : TestStep(iteration, true) {
    override fun toJson() = defaultToJson()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DiscardedTestStep

        if (receiver != other.receiver) return false
        if (!args.contentEquals(other.args)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = receiver?.hashCode() ?: 0
        result = 31 * result + args.contentHashCode()
        return result
    }
}

/**
 * Represents the output of an entire testing run.
 */
data class TestRunOutput(
    /** The seed that this testing run used. */
    val seed: Long,
    /** The reference class for this testing run. */
    val referenceClass: Class<*>,
    /** The submission class for this testing run. */
    val testedClass: Class<*>,
    /** The [Solution.name] of the @[Solution] annotation that this test used. */
    val solutionName: String,
    /** The time (in ms since epoch) that this test run started. Only the main testing loop is considered. */
    val startTime: Long,
    /** The time (in ms since epoch) that this test run ended. Only the main testing loop is considered. */
    val endTime: Long,
    /** Whether or not this test run ended in a time-out. */
    val timedOut: Boolean,
    /** The number of discarded test cases. */
    val numDiscardedTests: Int,
    /** The number of non-discarded tests which were executed. */
    val numTests: Int,
    /** The number of tests which contained only edge cases. */
    val numEdgeCaseTests: Int,
    /** The number of tests which contained only simple cases. */
    val numSimpleCaseTests: Int,
    /** The number of tests which contained a mix of simple and edge cases. */
    val numSimpleAndEdgeCaseTests: Int,
    /** The number of tests which contained a mix of edge, simple, and generated cases. */
    val numMixedTests: Int,
    /** The number of tests which contained purely generated inputs. */
    val numAllGeneratedTests: Int,
    /** The results of class design analysis between the [referenceClass] and [testedClass]. */
    val classDesignAnalysisResult: List<AnalysisOutput>,
    /** The list of [TestStep]s that were performed during this test run. */
    val testSteps: List<TestStep>
) : DefaultSerializable {
    override fun toJson() = defaultToJson()
}

data class TestingBlockCounts(
    var discardedTests: Int = 0,
    var edgeTests: Int = 0,
    var simpleTests: Int = 0,
    var simpleEdgeMixedTests: Int = 0,
    var generatedMixedTests: Int = 0,
    var allGeneratedTests: Int = 0) {

    val numTests: Int
        get() = edgeTests + simpleTests + simpleEdgeMixedTests + generatedMixedTests + allGeneratedTests
}
