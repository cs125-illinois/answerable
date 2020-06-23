package edu.illinois.cs.cs125.answerable.testing

import edu.illinois.cs.cs125.answerable.api.OssifiedTestOutput
import edu.illinois.cs.cs125.answerable.api.OssifiedValue
import edu.illinois.cs.cs125.answerable.api.TestOutput
import edu.illinois.cs.cs125.answerable.classdesignanalysis.CDAResult

/**
 * The types of behaviors that methods under test can have.
 */
@Suppress("unused")
enum class Behavior { RETURNED, THREW, VERIFY_ONLY, GENERATION_FAILED }

/**
 * Represents a single iteration of the main testing loop.
 */
sealed class TestStep(
    /** The number of the test represented by this [TestStep]. */
    val testNumber: Int,
    /** Whether or not this test case was discarded. */
    val wasDiscarded: Boolean,
    /** The test type */
    val testType: TestType
)

/**
 * Represents a test case that was executed.
 */
@Suppress("LongParameterList", "Unused")
class ExecutedTestStep(
    iteration: Int,
    testType: TestType,
    /** The receiver object passed to the reference. */
    val refReceiver: OssifiedValue?,
    /** The receiver object passed to the submission. */
    val subReceiver: OssifiedValue?,
    /** The receiver object passed to the reference. */
    val refLiveReceiver: Any?,
    /** The receiver object passed to the submission. */
    val subDangerousLiveReceiver: Any?,
    /** Whether or not the test case succeeded. */
    val succeeded: Boolean,
    /** The behavior of the reference solution. */
    val refOutput: OssifiedTestOutput,
    /** The behavior of the submission. */
    val subOutput: OssifiedTestOutput,
    /** The behavior of the reference solution,
     * including live objects with potentially computationally expensive behaviors. */
    val refLiveOutput: TestOutput<Any?>,
    /** The behavior of the submission, including live (untrusted!) objects. */
    val subDangerousLiveOutput: TestOutput<Any?>,
    /** The assertion error thrown, if any, by the verifier. */
    val assertErr: Throwable?
) : TestStep(iteration, false, testType)

/**
 * Represents a discarded test case.
 *
 * Test cases can only be discarded by failed preconditions, and preconditions
 * are always tested using the reference class. Thus, live objects contained
 * within are always safe.
 */
// "ossified" fields are needed by the Moshi serializer, but they can only
// ossified while a type pool is still available. Thus, the test step carries
// them, even though it isn't necessary to _expose_ them.
class DiscardedTestStep(
    iteration: Int,
    testType: TestType,
    /** The (ossified) receiver object that was passed to the precondition. */
    internal val ossifiedReceiver: OssifiedValue?,
    /** The (live) receiver object that was passed to the precondition. */
    val receiver: Any?,
    /** The other (ossified) arguments that were passed to the precondition. */
    internal val ossifiedArgs: Array<OssifiedValue?>,
    /** The other (live) arguments that were passed to the precondition. */
    val args: Array<Any?>
) : TestStep(iteration, true, testType) {
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
@Suppress("KDocUnresolvedReference", "unused")
data class TestingResults(
    /** The seed that this testing run used. */
    val seed: Long,
    /** The test runner arguments used. */
    val testRunnerArgs: TestRunnerArgs,
    /** The reference class for this testing run. */
    val referenceClass: Class<*>,
    /** The submission class for this testing run. */
    val testedClass: Class<*>,
    /** The name of the method that was tested if it was an @Solution; null otherwise. */
    val testedMethodName: String?,
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
    val classDesignAnalysisResult: CDAResult,
    /** The list of [TestStep]s that were performed during this test run. */
    val testSteps: List<TestStep>
) {
    @delegate:Transient
    val executedTestSteps: List<ExecutedTestStep> by lazy { testSteps.filterIsInstance<ExecutedTestStep>() }

    @delegate:Transient
    val succeeded: Boolean by lazy {
        classDesignAnalysisResult.allMatch &&
            executedTestSteps.size == testRunnerArgs.numTests &&
            executedTestSteps.all { it.succeeded }
    }

    @delegate:Transient
    val numFailedTests: Int by lazy {
        executedTestSteps.count { !it.succeeded }
    }

    fun assertAllSucceeded() {
        check(classDesignAnalysisResult.allMatch) { "Class design analysis failed" }
        check(executedTestSteps.size == testRunnerArgs.numTests) {
            "Fewer than the requested number of tests were run: ${executedTestSteps.size} < ${testRunnerArgs.numTests}"
        }
        testSteps.filterIsInstance<ExecutedTestStep>().also {
            check(it.isNotEmpty()) { "No tests were executed" }
        }.forEach {
            check(it.succeeded) { "Test failed: $it" }
        }
    }

    fun assertSomethingFailed() {
        val failed = try {
            assertAllSucceeded()
            false
        } catch (e: IllegalStateException) {
            true
        }
        check(failed) { "Nothing failed" }
    }
}

enum class TestType {
    Edge,
    Simple,
    EdgeSimpleMixed,
    Generated,
    GeneratedMixed,
    Regression
}

data class TestingBlockCounts(
    var discardedTests: Int = 0,
    var edgeTests: Int = 0,
    var simpleTests: Int = 0,
    var simpleEdgeMixedTests: Int = 0,
    var generatedMixedTests: Int = 0,
    var allGeneratedTests: Int = 0,
    var regressionTests: Int = 0
) {
    val numTests: Int
        get() = edgeTests + simpleTests + simpleEdgeMixedTests +
            generatedMixedTests + allGeneratedTests + regressionTests
}
