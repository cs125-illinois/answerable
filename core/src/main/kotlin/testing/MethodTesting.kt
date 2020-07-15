package edu.illinois.cs.cs125.answerable.testing

import edu.illinois.cs.cs125.answerable.TestEnvironment
import edu.illinois.cs.cs125.answerable.api.TestOutput
import edu.illinois.cs.cs125.answerable.api.ossify
import edu.illinois.cs.cs125.answerable.classmanipulation.TypePool
import edu.illinois.cs.cs125.answerable.classmanipulation.makeJavaProxy
import edu.illinois.cs.cs125.answerable.classmanipulation.makeValueProxy
import edu.illinois.cs.cs125.answerable.isPrinter
import org.junit.jupiter.api.Assertions.assertEquals
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.Random

internal class MethodTester(
    val referenceGenerator: MethodTestGenerator,
    val submissionGenerator: MethodTestGenerator,

    val referenceRandom: Random,
    val submissionRandom: Random,

    val verifier: (TestOutput<*>, TestOutput<*>) -> Unit = ::defaultVerifier
) {
    private val referenceMethod = referenceGenerator.method
    private val capturePrint = referenceMethod.isPrinter()
    private val referenceClass = referenceMethod.declaringClass
    private val submissionClass = submissionGenerator.method.declaringClass

    fun test(
        testNumber: Int,
        kind: TestKind,
        referenceReceiver: Any?,
        submissionReceiver: Any?,
        environment: TestEnvironment
    ): TestStepV2 {
        fun runOne(
            receiver: Any?,
            generator: MethodTestGenerator,
            random: Random
        ): TestOutput<Any?> {
            var behavior: Behavior? = null
            var threw: Throwable? = null
            var outText: String? = null
            var errText: String? = null
            var output: Any? = null

            val args: Array<Any?> = generator.generate(kind, random)

            val toRun = Runnable {
                try {
                    output = generator.method(receiver, *args)
                    behavior = Behavior.RETURNED
                } catch (e: InvocationTargetException) {
                    val cause = e.cause
                    if (cause is ThreadDeath) throw cause
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

            return TestOutput(
                typeOfBehavior = behavior!!,
                receiver = referenceClass.makeJavaProxy(receiver),
                args = args.map { makeValueProxy(referenceClass, submissionClass, it) }.toTypedArray(),
                output = makeValueProxy(referenceClass, submissionClass, output),
                threw = threw,
                stdOut = outText,
                stdErr = errText
            )
        }

        val referenceBehavior = runOne(referenceReceiver, referenceGenerator, referenceRandom)
        val submissionBehavior = runOne(submissionReceiver, submissionGenerator, submissionRandom)

        var assertErr: AssertionError? = null
        try {
            verifier(referenceBehavior, submissionBehavior)
            // AssertionError is from java.lang, and any reasonable testing library should extend it
        } catch(e: AssertionError) {
            assertErr = e
        }

        return TestStepV2(
            testNumber = testNumber,
            testKind = kind,
            referenceReceiver = referenceReceiver.ossify(),
            submissionReceiver = submissionReceiver.ossify(),
            referenceOutput = referenceBehavior.ossify(),
            submissionOutput = submissionBehavior.ossify(),
            referenceLiveReceiver = referenceReceiver,
            submissionDangerousLiveReceiver = submissionReceiver,
            referenceLiveOutput = referenceBehavior,
            submissionDangerousLiveOutput = submissionBehavior,

            assertionError = assertErr,
            succeeded = assertErr == null
        )
    }
}

// TODO: Verifier interface, better messages!
internal fun defaultVerifier(ours: TestOutput<*>, theirs: TestOutput<*>) {
    assertEquals(ours.threw?.javaClass, theirs.threw?.javaClass)
    assertEquals(ours.output, theirs.output)
    assertEquals(ours.stdOut, theirs.stdOut)
    assertEquals(ours.stdErr, theirs.stdErr)
}

internal class MethodTestGenerator(
    val edgeCases: MethodArgumentCases,
    val simpleCases: MethodArgumentCases,
    val generator: MethodArgumentGenerator,
    val method: Method
) {
    /**
     * Generate an input case for the given testing block.
     *
     * Some internal state is maintained for edge and simple cases.
     */
    fun generate(kind: TestKind, random: Random): Array<Any?> = when (kind) {
        is TestKind.EdgeCase -> generateEdgeTest(random)
        is TestKind.SimpleCase -> generateSimpleTest(random)
        is TestKind.Generated -> generateRandomTest(kind.complexity, random)
        is TestKind.Regression -> generateRandomTest(kind.complexity, random)
    }

    internal fun generateEdgeTest(random: Random): Array<Any?> =
        edgeCases.nextCase(random)

    internal fun generateSimpleTest(random: Random): Array<Any?> =
        simpleCases.nextCase(random)

    internal fun generateRandomTest(complexity: Int, random: Random): Array<Any?> =
        generator.generateParams(complexity, random)

    /**
     * Cases are enumerated without repeats, and without consideration for the receiver object.
     * When receiver objects are changed, the cases must be reset, or else you will not get cases
     * for the new receiver that were seen for previous receivers.
     */
    fun resetEdgeCases() = edgeCases.reset()
    /** See [resetEdgeCases]. */
    fun resetSimpleCases() = simpleCases.reset()
    /** See [resetEdgeCases]; resets both edge and simple cases. */
    fun resetCases() {
        resetEdgeCases()
        resetSimpleCases()
    }
}

internal class MethodTestGeneratorFactory(
    klass: Class<*>,
    pool: TypePool = TypePool(),
    controlClass: Class<*>? = null
) {
    private val edgeCaseMap = klass.edgeCaseMap(pool, controlClass)
    private val simpleCaseMap = klass.simpleCaseMap(pool, controlClass)
    private val generatorMap = klass.generatorMap(pool, controlClass)

    fun makeFor(method: Method): MethodTestGenerator =
        MethodTestGenerator(
            edgeCaseMap.casesForMethod(method, generatorMap),
            simpleCaseMap.casesForMethod(method, generatorMap),
            generatorMap.generatorForMethod(method),
            method
        )
}

// TODO: move this to the state machine file
sealed class TestKind {
    object EdgeCase : TestKind()
    object SimpleCase : TestKind()
    data class Generated(val complexity: Int) : TestKind()
    // complexity is expected to vary with complexity of generated cases
    data class Regression(val complexity: Int) : TestKind()
}
