@file:Suppress("SpreadOperator", "KDocUnresolvedReference")

package edu.illinois.cs.cs125.answerable

import edu.illinois.cs.cs125.answerable.annotations.DEFAULT_EMPTY_NAME
import edu.illinois.cs.cs125.answerable.annotations.DefaultTestRunArguments
import edu.illinois.cs.cs125.answerable.annotations.Solution
import edu.illinois.cs.cs125.answerable.annotations.Verify
import edu.illinois.cs.cs125.answerable.annotations.getAllNamedGenerators
import edu.illinois.cs.cs125.answerable.annotations.getEnabledGenerators
import edu.illinois.cs.cs125.answerable.annotations.getGroupedGenerator
import edu.illinois.cs.cs125.answerable.annotations.getPrecondition
import edu.illinois.cs.cs125.answerable.annotations.getSolution
import edu.illinois.cs.cs125.answerable.annotations.getTimeout
import edu.illinois.cs.cs125.answerable.annotations.getVerify
import edu.illinois.cs.cs125.answerable.annotations.validateAnnotations
import edu.illinois.cs.cs125.answerable.api.BytecodeProvider
import edu.illinois.cs.cs125.answerable.api.TestOutput
import edu.illinois.cs.cs125.answerable.api.ossify
import edu.illinois.cs.cs125.answerable.classdesignanalysis.CDAConfig
import edu.illinois.cs.cs125.answerable.classdesignanalysis.CDAResult
import edu.illinois.cs.cs125.answerable.classdesignanalysis.answerableName
import edu.illinois.cs.cs125.answerable.classdesignanalysis.classDesignAnalysis
import edu.illinois.cs.cs125.answerable.classdesignanalysis.noCDAResult
import edu.illinois.cs.cs125.answerable.classmanipulation.TypePool
import edu.illinois.cs.cs125.answerable.classmanipulation.mkGeneratorMirrorClass
import edu.illinois.cs.cs125.answerable.classmanipulation.mkOpenMirrorClass
import edu.illinois.cs.cs125.answerable.classmanipulation.mkProxy
import edu.illinois.cs.cs125.answerable.classmanipulation.mkValueProxy
import edu.illinois.cs.cs125.answerable.classmanipulation.verifyMemberAccess
import edu.illinois.cs.cs125.answerable.testing.Behavior
import edu.illinois.cs.cs125.answerable.testing.CustomGen
import edu.illinois.cs.cs125.answerable.testing.DiscardedTestStep
import edu.illinois.cs.cs125.answerable.testing.ExecutedTestStep
import edu.illinois.cs.cs125.answerable.testing.GenWrapper
import edu.illinois.cs.cs125.answerable.testing.GeneratorMap
import edu.illinois.cs.cs125.answerable.testing.GeneratorMapBuilder
import edu.illinois.cs.cs125.answerable.testing.GeneratorType
import edu.illinois.cs.cs125.answerable.testing.TestRunnerArgs
import edu.illinois.cs.cs125.answerable.testing.TestStep
import edu.illinois.cs.cs125.answerable.testing.TestType
import edu.illinois.cs.cs125.answerable.testing.TestingBlockCounts
import edu.illinois.cs.cs125.answerable.testing.TestingResults
import edu.illinois.cs.cs125.answerable.testing.asTestRunnerArgs
import edu.illinois.cs.cs125.answerable.testing.defaultArgs
import org.junit.jupiter.api.Assertions.assertEquals
import org.opentest4j.AssertionFailedError
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Type
import java.util.Random
import kotlin.math.min
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
 * a [BytecodeProvider] must be specified that can determine its bytecode.
 */
class TestGenerator(
    val referenceClass: Class<*>,
    val solutionName: String = DEFAULT_EMPTY_NAME,
    testRunnerArgs: TestRunnerArgs = defaultArgs,
    internal val bytecodeProvider: BytecodeProvider? = null
) {
    /**
     * A secondary constructor which uses Answerable's [defaultArgs] and no custom bytecode provider.
     */
    constructor(referenceClass: Class<*>, solutionName: String) : this(
        referenceClass, solutionName,
        defaultArgs
    )

    init {
        if ('$' in referenceClass.name) {
            throw AnswerableMisuseException("Reference class names cannot contain '$', sorry.")
        }
    }

    // "Usable" members are from the opened (un-final-ified) mirror of the original reference class.
    // The original members are used for certain checks so a nice class name can be displayed.

    private val languageMode = referenceClass.languageMode()
    internal val typePool = TypePool(
        bytecodeProvider,
        if (referenceClass.classLoader == javaClass.classLoader) {
            javaClass.classLoader
        } else {
            referenceClass.classLoader?.parent ?: javaClass.classLoader
        }
    )
    private val controlClass: Class<*> = languageMode.findControlClass(referenceClass, typePool) ?: referenceClass

    init {
        referenceClass.validateAnnotations(controlClass)
    }

    internal val usableReferenceClass: Class<*> = mkOpenMirrorClass(referenceClass, typePool, "openref_")
    internal val usableControlClass: Class<*> =
        if (controlClass == referenceClass) usableReferenceClass
        else mkOpenMirrorClass(controlClass, mapOf(referenceClass to usableReferenceClass), typePool, "controlmirror_")
    internal val usableReferenceMethod: Method? = usableReferenceClass.getSolution(solutionName)

    private val referenceMethod: Method? = referenceClass.getSolution(solutionName)
    internal val enabledNames: Array<String> =
        referenceMethod?.getAnnotation(Solution::class.java)?.enabled ?: arrayOf()

    internal val usablePrecondition: Method? = usableControlClass.getPrecondition(solutionName)
    private val customVerifier: Method? = controlClass.getVerify(solutionName)
    internal val usableCustomVerifier: Method? = usableControlClass.getVerify(solutionName)
    internal val mergedArgs: TestRunnerArgs

    init {
        if (referenceMethod == null) {
            if (customVerifier == null) {
                throw AnswerableMisuseException(
                    "No @Solution annotation or @Verify annotation with name `$solutionName' was found."
                )
            } else if (!customVerifier.getAnnotation(Verify::class.java)!!.standalone) {
                throw AnswerableMisuseException(
                    "No @Solution annotation with name `$solutionName' was found.\nPerhaps you meant" +
                        "to make verifier `${customVerifier.answerableName()}' standalone?"
                )
            }
        }
        val solutionArgsAnnotation = usableReferenceMethod?.getAnnotation(DefaultTestRunArguments::class.java)
        val verifyArgsAnnotation = usableCustomVerifier?.getAnnotation(DefaultTestRunArguments::class.java)
        if (solutionArgsAnnotation != null && verifyArgsAnnotation != null) {
            throw AnswerableMisuseException(
                "The @Solution and @Verify methods cannot both specify a @DefaultTestRunArguments.\n" +
                    "While loading question `$solutionName'."
            )
        }
        val argsAnnotation = solutionArgsAnnotation ?: verifyArgsAnnotation
        mergedArgs = testRunnerArgs.applyOver(argsAnnotation?.asTestRunnerArgs() ?: defaultArgs)
    }

    internal val atNextMethod: Method? = usableControlClass.getAtNext(enabledNames)
    internal val defaultConstructor: Constructor<*>? =
        usableReferenceClass.constructors.firstOrNull { it.parameterCount == 0 }

    internal val isStatic = referenceMethod?.let { Modifier.isStatic(it.modifiers) } ?: false

    internal val params = usableReferenceMethod?.getAnswerableParams() ?: arrayOf()
    internal val paramsWithReceiver = arrayOf(GeneratorType(usableReferenceClass), *params)

    internal val random: Random = Random(0)
    internal val generators: GeneratorMap = buildGeneratorMap(random)
    internal val edgeCases: Map<Type, ArrayWrapper?> =
        getEdgeCases(usableControlClass, paramsWithReceiver.map { it.type }.toTypedArray())
    internal val simpleCases: Map<Type, ArrayWrapper?> =
        getSimpleCases(usableControlClass, paramsWithReceiver.map { it.type }.toTypedArray())

    internal val timeout = referenceMethod?.getTimeout() ?: customVerifier?.getTimeout() ?: 0

    // Default constructor case is for when there is no @Generator and no @Next, but
    // we can still construct receiver objects via a default constructor.
    internal enum class ReceiverGenStrategy { GENERATOR, NEXT, DEFAULTCONSTRUCTOR, NONE }

    internal val receiverGenStrategy: ReceiverGenStrategy = when {
        isStatic -> ReceiverGenStrategy.NONE
        atNextMethod != null -> ReceiverGenStrategy.NEXT
        usableReferenceClass in generators.keys.map { it.type } -> ReceiverGenStrategy.GENERATOR
        defaultConstructor != null -> ReceiverGenStrategy.DEFAULTCONSTRUCTOR
        else -> throw AnswerableMisuseException(
            "The reference solution must provide either an @Generator or an @Next method " +
                "if @Solution is not static and no zero-argument constructor is accessible."
        )
    }

    init {
        verifySafety()
    }

    internal fun buildGeneratorMap(
        random: Random,
        submittedClassGenerator: Method? = null
    ): GeneratorMap {
        val generatorMapBuilder = GeneratorMapBuilder(
            params,
            random,
            typePool,
            if (isStatic) null else usableReferenceClass,
            languageMode
        )

        usableControlClass.getEnabledGenerators(enabledNames).map {
            if (it.returnType == usableReferenceClass && submittedClassGenerator != null) {
                Pair(GeneratorType(it.genericReturnType), CustomGen(submittedClassGenerator))
            } else {
                Pair(GeneratorType(it.genericReturnType), CustomGen(it))
            }
        }.also { enabledGens ->
            enabledGens.groupBy { it.first }.forEach { gensForType ->
                if (gensForType.value.size > 1) throw AnswerableMisuseException(
                    "Found multiple enabled generators for type `${gensForType.key.type.sourceName}'."
                )
            }
        }.forEach(generatorMapBuilder::accept)

        // The map builder needs to be aware of all named generators for parameter-specific generator choices
        usableControlClass.getAllNamedGenerators().map { (name, method) ->
            if (method.returnType == usableReferenceClass && submittedClassGenerator != null) {
                Pair(GeneratorType(method.genericReturnType, name), CustomGen(submittedClassGenerator))
            } else {
                Pair(GeneratorType(method.genericReturnType, name), CustomGen(method))
            }
        }.forEach(generatorMapBuilder::accept)

        return generatorMapBuilder.build().also { generatorMap ->
            usableControlClass.getGroupedGenerator(solutionName)?.also {
                generatorMap.parameterGenerator = GenWrapper(CustomGen(it), random)
            }
        }
    }

    private fun getEdgeCases(clazz: Class<*>, types: Array<Type>): Map<Type, ArrayWrapper?> {
        val all = languageMode.defaultEdgeCases + clazz.getEnabledEdgeCases(enabledNames)
        return mapOf(*types.map { it to all[it] }.toTypedArray())
    }

    private fun getSimpleCases(clazz: Class<*>, types: Array<Type>): Map<Type, ArrayWrapper?> {
        val all = languageMode.defaultSimpleCases + clazz.getEnabledSimpleCases(enabledNames)
        return mapOf(*types.map { it to all[it] }.toTypedArray())
    }

    private fun verifySafety() {
        verifyMemberAccess(referenceClass, typePool)

        @Suppress("MagicNumber")
        val dryRunOutput = PassedClassDesignRunner(
            this,
            mkOpenMirrorClass(referenceClass, typePool, "dryrunopenref_"),
            noCDAResult, mergedArgs, typePool.getLoader(),
            timeoutOverride = 10000
        ).runTestsUnsecured(0x0403)

        if (dryRunOutput.timedOut) {
            throw AnswerableVerificationException("Testing reference against itself timed out (10s).")
        }

        synchronized(dryRunOutput.testSteps) {
            dryRunOutput.testSteps.filterIsInstance(ExecutedTestStep::class.java).forEach {
                if (!it.succeeded) {
                    throw AnswerableVerificationException(
                        "Testing reference against itself failed on inputs: ${it.refOutput.args.contentDeepToString()}",
                        it.assertErr
                    )
                }
            }
        }
    }

    /**
     * Load a submission class to the problem represented by this [TestGenerator].
     *
     * The submission class will be run through Class Design Analysis against the reference solution.
     * The results of class design analysis will be included in the output of every test run by the [TestRunner]
     * returned. If class design analysis fails, the returned [TestRunner] will never execute any tests, as doing so
     * would be unsafe and cause nasty errors.
     *
     * @param submissionClass the class to be tested against the reference
     * @param testRunnerArgs the arguments that the [TestRunner] returned should default to.
     * @param bytecodeProvider provider of bytecode for the submission class(es), or null if not loaded dynamically
     */
    fun loadSubmission(
        submissionClass: Class<*>,
        testRunnerArgs: TestRunnerArgs = defaultArgs,
        bytecodeProvider: BytecodeProvider? = null
    ): TestRunner {
        val cdaResult = classDesignAnalysis(
            referenceClass,
            submissionClass,
            CDAConfig(solutionName = solutionName)
        )
        val cdaPassed = cdaResult.allMatch

        return if (cdaPassed) {
            PassedClassDesignRunner(
                this,
                submissionClass,
                cdaResult,
                testRunnerArgs.applyOver(this.mergedArgs),
                bytecodeProvider
            )
        } else {
            FailedClassDesignTestRunner(referenceClass, solutionName, submissionClass, cdaResult)
        }
    }
}

/**
 * Represents a class that can execute tests.
 */
interface TestRunner {
    fun runTests(seed: Long, environment: TestEnvironment, testRunnerArgs: TestRunnerArgs): TestingResults
    fun runTests(seed: Long, environment: TestEnvironment): TestingResults
}

/**
 * Perform a test run in an unsecured environment.
 *
 * This function is inherently dangerous and allows a submission class to run arbitrary code
 * on your JVM! Prefer passing a secured environment to runTests.
 */
fun TestRunner.runTestsUnsecured(seed: Long, testRunnerArgs: TestRunnerArgs = defaultArgs) =
    this.runTests(seed, unsecuredEnvironment, testRunnerArgs)

/**
 * The primary [TestRunner] subclass which tests classes that have passed Class Design Analysis.
 *
 * The only publicly-exposed way to create a [PassedClassDesignRunner] is to invoke
 * [TestGenerator.loadSubmission] on an existing [TestGenerator].
 */
class PassedClassDesignRunner internal constructor(
    private val testGenerator: TestGenerator,
    private val submissionClass: Class<*>,
    private val cachedClassDesignAnalysisResult: CDAResult = noCDAResult,
    private val testRunnerArgs: TestRunnerArgs, // Already merged by TestGenerator#loadSubmission
    private val bytecodeProvider: BytecodeProvider?,
    private val timeoutOverride: Long? = null
) : TestRunner {

    internal constructor(
        testGenerator: TestGenerator,
        submissionClass: Class<*>,
        cdaResult: CDAResult = noCDAResult,
        testRunnerArgs: TestRunnerArgs = defaultArgs
    ) : this(testGenerator, submissionClass, cdaResult, testRunnerArgs, null)

    internal constructor(
        referenceClass: Class<*>,
        submissionClass: Class<*>,
        cdaResult: CDAResult = noCDAResult,
        testRunnerArgs: TestRunnerArgs = defaultArgs
    ) : this(TestGenerator(referenceClass), submissionClass, cdaResult, testRunnerArgs)

    /**
     * [TestRunner.runTests] override which accepts [TestRunnerArgs]. Executes a test suite.
     *
     * If the method under test has a timeout, [runTests] will run as many tests as it can before the timeout
     * is reached, and record the results.
     *
     * When called with the same [seed], [runTests] will always produce the same result.
     */
    override fun runTests(seed: Long, environment: TestEnvironment, testRunnerArgs: TestRunnerArgs): TestingResults {
        val submissionTypePool = TypePool(bytecodeProvider, submissionClass.classLoader)
        val untrustedSubMirror = mkOpenMirrorClass(submissionClass, submissionTypePool, "opensub_")
        val loader = environment.sandbox.transformLoader(submissionTypePool.getLoader())
        val sandboxedSubMirror = Class.forName(untrustedSubMirror.name, false, loader.getLoader())
        val worker = TestRunWorker(testGenerator, sandboxedSubMirror, environment, loader, submissionTypePool)
        val timeLimit = timeoutOverride ?: testGenerator.timeout

        // Store reference class static field values so that the next run against this solution doesn't break
        val refStaticFieldValues = testGenerator.usableReferenceClass.declaredFields
            .filter { Modifier.isStatic(it.modifiers) && !Modifier.isFinal(it.modifiers) }.map {
                it.isAccessible = true
                it to it.get(null)
            }

        val testSteps = mutableListOf<TestStep>()
        val testingBlockCounts = TestingBlockCounts()
        val startTime = System.currentTimeMillis()

        val mergedArgs = testRunnerArgs.applyOver(this.testRunnerArgs)

        // the tests are executed here
        val timedOut = !environment.sandbox.run(
            if (timeLimit == 0L) null else timeLimit,
            Runnable {
                worker.runTests(seed, mergedArgs, testSteps, testingBlockCounts)
            }
        )
        val endTime = System.currentTimeMillis()

        // Restore reference class static field values
        refStaticFieldValues.forEach { (field, value) -> field.set(null, value) }

        return TestingResults(
            seed = seed,
            testRunnerArgs = mergedArgs.resolve(),
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
            testSteps = synchronized(testSteps) { testSteps.toList() }
        )
    }

    /**
     * [TestRunner.runTests] overload which uses the [TestRunnerArgs] that this [PassedClassDesignRunner]
     * was constructed with.
     */
    override fun runTests(seed: Long, environment: TestEnvironment) =
        runTests(seed, environment, this.testRunnerArgs) // to expose the overload to Java
}

internal class TestRunWorker internal constructor(
    private val testGenerator: TestGenerator,
    private val usableSubmissionClass: Class<*>,
    private val environment: TestEnvironment,
    private val bytecodeProvider: BytecodeProvider?,
    private val untrustedSubmissionTypePool: TypePool
) {
    private val usableReferenceClass = testGenerator.usableReferenceClass
    private val usableControlClass = testGenerator.usableControlClass
    private val usableReferenceMethod = testGenerator.usableReferenceMethod
    private val usableCustomVerifier = testGenerator.usableCustomVerifier

    @Suppress("MagicNumber")
    private val passRandomToVerify = usableCustomVerifier?.parameters?.size == 3

    private val submissionTypePool = TypePool(bytecodeProvider, usableSubmissionClass.classLoader)
        .also { it.takeOriginalClassMappings(untrustedSubmissionTypePool, usableSubmissionClass.classLoader) }
    private val adapterTypePool = TypePool(testGenerator.typePool, submissionTypePool)
    private val usableSubmissionMethod =
        usableSubmissionClass.findSolutionAttemptMethod(usableReferenceMethod, usableReferenceClass)

    private val params = testGenerator.params
    private val paramsWithReceiver = testGenerator.paramsWithReceiver

    private val precondition = testGenerator.usablePrecondition

    private val testRunnerRandom = Random(0)
    private val randomForReference = testGenerator.random
    private val randomForSubmission = Random(0)

    private val generatorMirrorToStudentClass =
        if (testGenerator.usableControlClass == testGenerator.usableReferenceClass) {
            mkGeneratorMirrorClass(usableReferenceClass, usableSubmissionClass, adapterTypePool, "genmirror_")
        } else {
            mkOpenMirrorClass(
                usableControlClass,
                mapOf(usableReferenceClass to usableSubmissionClass),
                adapterTypePool,
                "controlgenmirror_"
            )
        }

    private val referenceAtNext = testGenerator.atNextMethod
    private val submissionAtNext = generatorMirrorToStudentClass.getAtNext(testGenerator.enabledNames)
    private val referenceDefaultCtor = testGenerator.defaultConstructor
    private val submissionDefaultCtor = usableSubmissionClass.constructors.firstOrNull { it.parameterCount == 0 }

    private val referenceEdgeCases = testGenerator.edgeCases
    private val referenceSimpleCases = testGenerator.simpleCases
    private val submissionEdgeCases: Map<Type, ArrayWrapper?> = referenceEdgeCases
        // replace reference class cases with mirrored cases
        // the idea is that each map takes `params` to the correct generator/cases
        .toMutableMap().apply {
            replace(
                usableReferenceClass,
                generatorMirrorToStudentClass.getEnabledEdgeCases(testGenerator.enabledNames)[usableSubmissionClass]
            )
        }
    private val submissionSimpleCases: Map<Type, ArrayWrapper?> = referenceSimpleCases
        // replace reference class cases with mirrored cases
        .toMutableMap().apply {
            replace(
                usableReferenceClass,
                generatorMirrorToStudentClass.getEnabledSimpleCases(testGenerator.enabledNames)[usableSubmissionClass]
            )
        }

    private val referenceGens = testGenerator.generators
    private val submissionGens = generatorMirrorToStudentClass
        .getEnabledGenerators(testGenerator.enabledNames)
        .find { it.returnType == usableSubmissionClass }
        .let { testGenerator.buildGeneratorMap(randomForSubmission, it) }

    private val receiverGenStrategy = testGenerator.receiverGenStrategy
    private val capturePrint = usableReferenceMethod?.isPrinter() ?: false
    private val isStatic = testGenerator.isStatic

    private fun calculateNumCases(cases: Map<Type, ArrayWrapper?>): Int =
        paramsWithReceiver.foldIndexed(1) { idx, acc, param ->
            cases[param.type]?.let { cases: ArrayWrapper ->
                (if (idx == 0) ((cases.array as? Array<*>)?.filterNotNull()?.size ?: 1) else cases.size).let {
                    return@foldIndexed acc * it
                }
            } ?: acc
        }

    private fun calculateCase(
        index: Int,
        total: Int,
        cases: Map<Type, ArrayWrapper?>,
        backups: Map<GeneratorType, GenWrapper<*>>
    ): Array<Any?> {
        var segmentSize = total
        var segmentIndex = index

        val case = Array<Any?>(paramsWithReceiver.size) { null }
        @Suppress("LoopWithTooManyJumpStatements")
        for (i in paramsWithReceiver.indices) {
            val param = paramsWithReceiver[i]
            val typeCases = cases[param.type]

            if (i == 0) { // receiver
                if (typeCases == null) {
                    case[0] = null
                    continue
                }
                val typeCasesArr = typeCases.array as? Array<*>
                    ?: throw IllegalStateException(
                        "Answerable thinks a receiver type is primitive. Please report a bug."
                    )
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
                    case[i] = backups[param]?.generate(0)
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
        backups: Map<GeneratorType, GenWrapper<*>>,
        random: Random
    ): Array<Any?> {
        val case = Array<Any?>(params.size) { null }
        for (i in params.indices) {
            val edge = random.nextInt(2) == 0
            var simple = !edge
            val param = params[i]
            val type = param.type

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
                    case[i] = backups[param]?.generate(if (edge) 0 else 2)
                }
            }
        }

        return case
    }

    private fun mkGeneratedMixedCase(
        edges: Map<Type, ArrayWrapper?>,
        simples: Map<Type, ArrayWrapper?>,
        gens: Map<GeneratorType, GenWrapper<*>>,
        complexity: Int,
        random: Random
    ): Array<Any?> {
        val case = Array<Any?>(params.size) { null }

        for (i in params.indices) {
            val param = params[i]
            val type = param.type

            @Suppress("MagicNumber")
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
                case[i] = (gens[param] ?: error("Missing generator for ${param.type.sourceName}")).generate(complexity)
            }
        }

        return case
    }

    @Suppress("LongParameterList")
    private fun testWith(
        iteration: Int,
        testType: TestType,
        refReceiver: Any?,
        subReceiver: Any?,
        refMethodArgs: Array<Any?>,
        subMethodArgs: Array<Any?>
    ): TestStep {

        var subProxy: Any? = null

        if (!isStatic) {
            subProxy = mkProxy(usableReferenceClass, usableSubmissionClass, subReceiver!!, testGenerator.typePool)
        }

        return test(iteration, testType, refReceiver, subReceiver, subProxy, refMethodArgs, subMethodArgs)
    }

    private fun mkRefReceiver(iteration: Int, complexity: Int, prevRefReceiver: Any?): Any? =
        when (receiverGenStrategy) {
            TestGenerator.ReceiverGenStrategy.NONE -> null
            TestGenerator.ReceiverGenStrategy.DEFAULTCONSTRUCTOR -> referenceDefaultCtor?.newInstance()
            TestGenerator.ReceiverGenStrategy.GENERATOR -> referenceGens[GeneratorType(usableReferenceClass)]?.generate(
                complexity
            )
            TestGenerator.ReceiverGenStrategy.NEXT -> referenceAtNext?.invoke(
                null,
                prevRefReceiver,
                iteration,
                randomForReference
            )
        }

    private fun mkSubReceiver(iteration: Int, complexity: Int, prevSubReceiver: Any?): Any? =
        when (receiverGenStrategy) {
            TestGenerator.ReceiverGenStrategy.NONE -> null
            TestGenerator.ReceiverGenStrategy.DEFAULTCONSTRUCTOR -> submissionDefaultCtor?.newInstance()
            TestGenerator.ReceiverGenStrategy.GENERATOR ->
                submissionGens[GeneratorType(usableReferenceClass)]?.generate(
                    complexity
                )
            TestGenerator.ReceiverGenStrategy.NEXT -> submissionAtNext?.invoke(
                null,
                prevSubReceiver,
                iteration,
                randomForSubmission
            )
        }

    @Suppress("NestedBlockDepth", "LongParameterList")
    private fun test(
        iteration: Int,
        testType: TestType,
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
                    } catch (e: InvocationTargetException) {
                        if (e.cause is ThreadDeath) throw ThreadDeath()
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
                    usableSubmissionClass.publicFields.forEach {
                        usableReferenceClass.getField(it.name).set(subProxy, it.get(subReceiver))
                    }
                }
                val subBehaviorWithProxiedArgs = subBehavior.copy(
                    args = subBehavior.args.map {
                        mkValueProxy(it, usableReferenceClass, usableSubmissionClass, submissionTypePool)
                    }.toTypedArray()
                )
                if (passRandomToVerify) {
                    usableCustomVerifier.invoke(null, refBehavior, subBehaviorWithProxiedArgs, testRunnerRandom)
                } else {
                    usableCustomVerifier.invoke(null, refBehavior, subBehaviorWithProxiedArgs)
                }
            }
        } catch (ite: InvocationTargetException) {
            assertErr = ite.cause
        } catch (e: AssertionFailedError) {
            assertErr = e
        }

        return ExecutedTestStep(
            iteration = iteration,
            testType = testType,
            refReceiver = refReceiver.ossify(testGenerator.typePool),
            subReceiver = subReceiver.ossify(submissionTypePool),
            refLiveReceiver = refReceiver,
            subDangerousLiveReceiver = subReceiver,
            succeeded = assertErr == null,
            refOutput = refBehavior.ossify(testGenerator.typePool),
            subOutput = subBehavior.ossify(submissionTypePool),
            refLiveOutput = refBehavior,
            subDangerousLiveOutput = subBehavior,
            assertErr = assertErr
        )
    }

    /* NOTE: [Testing Loop Critical Points]

    There are several important bits that the testing loop needs to hit. Obviously, if any particular test case fails,
    that's an out immediately. But because Java is a persistent state language, tests also need to verify that that
    persistent state is handled correctly.

    One important piece of that is regression testing. We need to ensure that student objects work when re-used.
    To clarify, let's refer to receiver objects by their index in the sequence of receiver objects used by Answerable.
    We might expect to see sequences like 0 1 2 3 4 5 6 7 8... and 0 0 0 0 ... 0 1 1 1 ... 1 2 2 2 ..., in other words,
    linear progressions. But actually, we need to see sequences that reuse old objects.

    Regression test count is configurable in DefaultTestRunArguments and inserts evenly-spaced regression tests
    throughout the whole testing loop. @Next methods need to receive objects that are *not* from regression tests,
    so those have to be saved across iterations.

    */
    @Suppress("ComplexMethod", "LongMethod")
    fun runTests(
        seed: Long,
        testRunnerArgs: TestRunnerArgs,
        testStepList: MutableList<TestStep>,
        testingBlockCounts: TestingBlockCounts
    ) {
        val resolvedArgs = testRunnerArgs.resolve() // All properties are non-null
        val numTests = resolvedArgs.numTests!!
        val numEdgeCombinations = calculateNumCases(referenceEdgeCases)
        val numSimpleCombinations = calculateNumCases(referenceSimpleCases)

        val numEdgeCaseTests = if (referenceEdgeCases.values.all { it.isNullOrEmpty() }) 0 else min(
            resolvedArgs.maxOnlyEdgeCaseTests!!,
            numEdgeCombinations
        )
        val edgeExhaustive = numEdgeCombinations <= resolvedArgs.maxOnlyEdgeCaseTests!!
        val numSimpleCaseTests = if (referenceSimpleCases.values.all { it.isNullOrEmpty() }) 0 else min(
            resolvedArgs.maxOnlySimpleCaseTests!!,
            numSimpleCombinations
        )
        val simpleExhaustive = numSimpleCombinations <= resolvedArgs.maxOnlySimpleCaseTests!!
        val numSimpleEdgeMixedTests = resolvedArgs.numSimpleEdgeMixedTests!!
        val numAllGeneratedTests = resolvedArgs.numAllGeneratedTests!!
        val numRegressionTests = resolvedArgs.numRegressionTests!!

        val numGeneratedMixedTests: Int = numTests -
            numEdgeCaseTests - numSimpleCaseTests - numSimpleEdgeMixedTests -
            numRegressionTests - numAllGeneratedTests

        setOf(randomForReference, randomForSubmission, testRunnerRandom).forEach { it.setSeed(seed) }

        var useRefReceiver: Any? // the receiver that should be used for the current test iteration
        var useSubReceiver: Any? // ^
        var nonRegressRefReceiver: Any? = null // the most recent receiver used in a non-regression test
        var nonRegressSubReceiver: Any? = null // ^

        val regressRefReceivers: MutableList<Any?> = mutableListOf() // receivers available for use in regression tests
        val regressSubReceivers: MutableList<Any?> = mutableListOf() // ^

        var block: TestType
        var generatedMixedIdx = 0
        var allGeneratedIdx = 0

        var i = 0
        while (testingBlockCounts.numTests < numTests) {
            // TODO: Catch exceptions from generation and report the test as Behavior.GENERATION_FAILED
            val refMethodArgs: Array<Any?>
            val subMethodArgs: Array<Any?>
            @Suppress("MagicNumber")
            when {
                (testingBlockCounts.numTests + 1) % 16 == 0 -> {
                    block = TestType.Regression

                    // TODO: Smarter way to pick regression receivers?
                    val receiverIx = testRunnerRandom.nextInt(regressRefReceivers.size)
                    useRefReceiver = regressRefReceivers[receiverIx]
                    useSubReceiver = regressSubReceivers[receiverIx]

                    // TODO: Use more complex arguments?
                    val comp = testRunnerRandom.nextInt(5) // 0 to 4, basically simple
                    refMethodArgs = params.map { referenceGens[it]?.generate(comp) }.toTypedArray()
                    subMethodArgs = params.map { submissionGens[it]?.generate(comp) }.toTypedArray()
                }
                testingBlockCounts.edgeTests < numEdgeCaseTests -> {
                    block = TestType.Edge

                    // if we can't exhaust the cases, duplicates are less impactful
                    val idx = if (edgeExhaustive) (testingBlockCounts.edgeTests)
                    else testRunnerRandom.nextInt(numEdgeCombinations)

                    val refCase = calculateCase(idx, numEdgeCombinations, referenceEdgeCases, referenceGens)
                    val subCase = calculateCase(idx, numEdgeCombinations, submissionEdgeCases, submissionGens)

                    refMethodArgs = refCase.slice(1..refCase.indices.last).toTypedArray()
                    subMethodArgs = subCase.slice(1..subCase.indices.last).toTypedArray()

                    useRefReceiver = if (refCase[0] != null) refCase[0] else mkRefReceiver(i, 0, nonRegressRefReceiver)
                    useSubReceiver = if (subCase[0] != null) subCase[0] else mkSubReceiver(i, 0, nonRegressSubReceiver)
                }
                testingBlockCounts.simpleTests < numSimpleCaseTests -> {
                    block = TestType.Simple
                    val idx = if (simpleExhaustive) (testingBlockCounts.simpleTests)
                    else testRunnerRandom.nextInt(numSimpleCombinations)

                    val refCase = calculateCase(idx, numSimpleCombinations, referenceSimpleCases, referenceGens)
                    val subCase = calculateCase(idx, numSimpleCombinations, submissionSimpleCases, submissionGens)

                    refMethodArgs = refCase.slice(1..refCase.indices.last).toTypedArray()
                    subMethodArgs = subCase.slice(1..subCase.indices.last).toTypedArray()

                    useRefReceiver = if (refCase[0] != null) refCase[0] else mkRefReceiver(i, 0, nonRegressRefReceiver)
                    useSubReceiver = if (subCase[0] != null) subCase[0] else mkSubReceiver(i, 0, nonRegressSubReceiver)
                }
                testingBlockCounts.simpleEdgeMixedTests < numSimpleEdgeMixedTests -> {
                    block = TestType.EdgeSimpleMixed

                    useRefReceiver = mkRefReceiver(i, 2, nonRegressRefReceiver)
                    useSubReceiver = mkSubReceiver(i, 2, nonRegressSubReceiver)

                    refMethodArgs = mkSimpleEdgeMixedCase(
                        referenceEdgeCases,
                        referenceSimpleCases,
                        referenceGens,
                        randomForReference
                    )
                    subMethodArgs = mkSimpleEdgeMixedCase(
                        submissionEdgeCases,
                        submissionSimpleCases,
                        submissionGens,
                        randomForSubmission
                    )
                }
                testingBlockCounts.allGeneratedTests < numAllGeneratedTests -> {
                    block = TestType.Generated

                    val comp = min(
                        (resolvedArgs.maxComplexity!! * allGeneratedIdx) / numAllGeneratedTests,
                        resolvedArgs.maxComplexity
                    )

                    useRefReceiver = mkRefReceiver(i, comp, nonRegressRefReceiver)
                    useSubReceiver = mkSubReceiver(i, comp, nonRegressSubReceiver)

                    refMethodArgs = referenceGens.generate(params, comp)
                    subMethodArgs = submissionGens.generate(params, comp)

                    allGeneratedIdx++
                }
                testingBlockCounts.numTests < numTests -> {
                    block = TestType.GeneratedMixed

                    val comp = min(
                        (resolvedArgs.maxComplexity!! * generatedMixedIdx) / numGeneratedMixedTests,
                        resolvedArgs.maxComplexity
                    )

                    useRefReceiver = mkRefReceiver(i, comp, nonRegressRefReceiver)
                    useSubReceiver = mkSubReceiver(i, comp, nonRegressSubReceiver)

                    refMethodArgs = mkGeneratedMixedCase(
                        referenceEdgeCases,
                        referenceSimpleCases,
                        referenceGens,
                        comp,
                        randomForReference
                    )
                    subMethodArgs = mkGeneratedMixedCase(
                        submissionEdgeCases,
                        submissionSimpleCases,
                        submissionGens,
                        comp,
                        randomForSubmission
                    )

                    generatedMixedIdx++
                }

                else ->
                    throw IllegalStateException(
                        "Answerable somehow lost proper track of test block counts. Please report a bug."
                    )
            }

            val preconditionMet: Boolean = (precondition?.invoke(useRefReceiver, *refMethodArgs) ?: true) as Boolean

            val result: TestStep
            if (preconditionMet) {
                result = testWith(i, block, useRefReceiver, useSubReceiver, refMethodArgs, subMethodArgs)
                if (block != TestType.Regression) {
                    regressRefReceivers.add(useRefReceiver)
                    regressSubReceivers.add(useSubReceiver)
                    nonRegressRefReceiver = useRefReceiver
                    nonRegressSubReceiver = useSubReceiver
                }
                when (block) {
                    TestType.Edge -> testingBlockCounts.edgeTests++
                    TestType.Simple -> testingBlockCounts.simpleTests++
                    TestType.EdgeSimpleMixed -> testingBlockCounts.simpleEdgeMixedTests++
                    TestType.Generated -> testingBlockCounts.allGeneratedTests++
                    TestType.GeneratedMixed -> testingBlockCounts.generatedMixedTests++
                    TestType.Regression -> testingBlockCounts.regressionTests++
                }
            } else {
                // We have to increment these if the precondition fails because otherwise we
                // will keep trying the same cases and end up killing the testing loop by discarding
                // the same case 1000 times.
                when (block) {
                    TestType.Edge -> testingBlockCounts.edgeTests++
                    TestType.Simple -> testingBlockCounts.simpleTests++
                    else -> Unit
                }
                result = DiscardedTestStep(
                    iteration = i,
                    testType = block,
                    ossifiedReceiver = useRefReceiver.ossify(testGenerator.typePool),
                    receiver = useRefReceiver,
                    ossifiedArgs = refMethodArgs.map { it.ossify(testGenerator.typePool) }.toTypedArray(),
                    args = refMethodArgs
                )
                testingBlockCounts.discardedTests++
            }
            synchronized(testStepList) {
                testStepList.add(result)
            }

            if (testingBlockCounts.discardedTests >= resolvedArgs.maxDiscards!!) break
            i++
        }
    }
}

/**
 * The secondary [TestRunner] subclass representing a [submissionClass] which failed Class Design Analysis
 * against the [referenceClass].
 *
 * [runTests] will always execute 0 tests and produce an empty [TestingResults.testSteps].
 * The class design analysis results will be contained in the output.
 */
class FailedClassDesignTestRunner(
    private val referenceClass: Class<*>,
    private val solutionName: String,
    private val submissionClass: Class<*>,
    private val failedCDAResult: CDAResult
) : TestRunner {
    override fun runTests(seed: Long, environment: TestEnvironment): TestingResults =
        TestingResults(
            seed = seed,
            testRunnerArgs = defaultArgs, // maybe we can do better here when we refactor...
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

    override fun runTests(seed: Long, environment: TestEnvironment, testRunnerArgs: TestRunnerArgs): TestingResults =
        runTests(seed, environment)
}

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
