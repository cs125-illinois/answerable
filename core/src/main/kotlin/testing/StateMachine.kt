package edu.illinois.cs.cs125.answerable.testing

import kotlin.math.roundToInt

// TODO: Update TestRunnerArgs (see comment there about transitive updates)
//  to match the usage here. Specifically: the only test kinds are now edge, simple,
//  all generated, and regression, and should be split up roughly 1/8,1/8,11/16,1/16
internal class StateMachine(private val baseTestRunnerArgs: TestRunnerArgs) : Iterator<TestKind> {
    private var runArguments: TestRunnerArgs = baseTestRunnerArgs.resolve()
    private var counter: TestKindCounter =
        TestKindCounter(runArguments.numTests!!)

    private class TestKindCounter(val testsToRun: Int) {
        var edgeCases: Int = 0
        var simpleCases: Int = 0
        var generatedTests: Int = 0
        var regressionTests: Int = 0

        val testsRun: Int
            get() = edgeCases + simpleCases + generatedTests + regressionTests

        val done: Boolean
            get() = testsRun >= testsToRun
    }

    fun initialize(testRunArguments: TestRunnerArgs) {
        runArguments = testRunArguments.applyOver(baseTestRunnerArgs).resolve()
        counter = TestKindCounter(runArguments.numTests!!)
        cachedScaleComplexityBy = null
        scalingComplexity = 0.0
    }

    fun notifyCasesExhausted(caseKind: CaseKind) {
        runArguments = when (caseKind) {
            CaseKind.EDGE -> runArguments.copy(maxOnlyEdgeCaseTests = counter.edgeCases)
            CaseKind.SIMPLE -> runArguments.copy(maxOnlySimpleCaseTests = counter.simpleCases)
        }
    }

    private val numTests: Int
        get() = runArguments.numTests!!
    private val maxEdgeCases: Int
        get() = runArguments.maxOnlyEdgeCaseTests!!
    private val maxSimpleCases: Int
        get() = runArguments.maxOnlySimpleCaseTests!!
    private val numRegressionTests: Int
        get() = runArguments.numRegressionTests!!
    private val regressionTestPeriod: Int
        get() = numTests / numRegressionTests

    override fun hasNext(): Boolean = !counter.done

    override fun next(): TestKind = when {
        // see [NOTE: counting for regression tests]
        (counter.testsRun + 1) % regressionTestPeriod == 0 -> {
            counter.regressionTests++
            TestKind.Regression(nextRegressionComplexity())
        }
        counter.edgeCases < maxEdgeCases -> {
            counter.edgeCases++
            TestKind.EdgeCase
        }
        counter.simpleCases < maxSimpleCases -> {
            counter.simpleCases++
            TestKind.SimpleCase
        }
        else -> {
            counter.generatedTests++
            TestKind.Generated(nextGeneratedComplexity())
        }
    }

    var scalingComplexity: Double = 0.0
    private fun nextRegressionComplexity(): Int = scalingComplexity.roundToInt()

    private val numGeneratedTests: Int
        get() = numTests - maxEdgeCases - maxSimpleCases - numRegressionTests
    private var cachedScaleComplexityBy: Double? = null
    private val scaleComplexityBy: Double
        // see comment below; the first time this is called each run, numGeneratedTests is fixed
        get() {
            if (cachedScaleComplexityBy == null) {
                cachedScaleComplexityBy = runArguments.maxComplexity!!.toDouble() / numGeneratedTests
            }
            return cachedScaleComplexityBy!!
        }
    // when this function is called, it means we're done with case tests, and should start scaling the complexity.
    // At this point, case exhaustion notifications won't change anything. In turn,
    // that means that numGeneratedTests is now a fixed value.
    private fun nextGeneratedComplexity(): Int {
        val ret = scalingComplexity.roundToInt()
        scalingComplexity += scaleComplexityBy
        return ret
    }
}

/* [NOTE: counting for regression tests]
We don't want iteration 0 to be a regression test, because there's nothing tested yet that could have
regressed. But we _do_ want iteration 1023 (or whatever the last iteration happens to be for this run)
to be a regression test. Normal modulo operation for doing things periodically like this biases the beginning
of the period. To bias the end of the period instead, we just have to add 1. Now iteration 0 won't be a regression test,
and iteration 1023 will.
 */