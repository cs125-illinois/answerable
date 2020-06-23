@file: JvmName("Arguments")

package edu.illinois.cs.cs125.answerable.testing

import edu.illinois.cs.cs125.answerable.annotations.DefaultTestRunArguments

/**
 * A class that holds configuration for [TestRunner]s.
 */
// If you modify this class, be sure to update the DefaultTestRunArguments annotation
// (in api/Annotations.kt) and the asTestRunnerArgs extension (below) to match!
data class TestRunnerArgs(
    /** The total number of tests to execute. Defaults to 1024. */
    val numTests: Int? = null,
    /** The maximum number of tests which can be discarded before Answerable gives up. Defaults to 1024. */
    val maxDiscards: Int? = null,
    /** The maximum number of edge-case only tests to execute. Defaults to [numTests] / 16. */
    val maxOnlyEdgeCaseTests: Int? = null,
    /** The maximum number of simple-case only tests to execute. Defaults to [numTests] / 16. */
    val maxOnlySimpleCaseTests: Int? = null,
    /** The total number of mixed edge- and simple-case tests to execute. Defaults to [numTests] / 16. */
    val numSimpleEdgeMixedTests: Int? = null,
    /** The total number of all-generated tests to execute. Defaults to [numTests] / 2. */
    val numAllGeneratedTests: Int? = null,
    /** The total number of regression tests to execute. Defaults to [numTests] / 16. */
    val numRegressionTests: Int? = null,
    /** The upper bound on the 'complexity' @[Generator] parameter for this test run. Defaults to 100. */
    val maxComplexity: Int? = null
) {
    fun applyOver(base: TestRunnerArgs): TestRunnerArgs {
        return TestRunnerArgs(
            numTests = numTests ?: base.numTests,
            maxDiscards = maxDiscards ?: base.maxDiscards,
            maxOnlyEdgeCaseTests = maxOnlyEdgeCaseTests ?: base.maxOnlyEdgeCaseTests,
            maxOnlySimpleCaseTests = maxOnlySimpleCaseTests ?: base.maxOnlySimpleCaseTests,
            numSimpleEdgeMixedTests = numSimpleEdgeMixedTests ?: base.numSimpleEdgeMixedTests,
            numAllGeneratedTests = numAllGeneratedTests ?: base.numAllGeneratedTests,
            numRegressionTests = numRegressionTests ?: base.numRegressionTests,
            maxComplexity = maxComplexity ?: base.maxComplexity
        )
    }

    fun resolve(): TestRunnerArgs {
        val resolvedNumTests = numTests ?: DEFAULT_NUM_TESTS
        return TestRunnerArgs(
            numTests = resolvedNumTests,
            maxDiscards = maxDiscards
                ?: DEFAULT_MAX_DISCARDS,
            maxOnlyEdgeCaseTests = maxOnlyEdgeCaseTests
                ?: resolvedNumTests / DEFAULT_MAX_FRACTION_EDGE_CASES,
            maxOnlySimpleCaseTests = maxOnlySimpleCaseTests
                ?: resolvedNumTests / DEFAULT_MAX_FRACTION_SIMPLE_CASES,
            numSimpleEdgeMixedTests = numSimpleEdgeMixedTests
                ?: resolvedNumTests / DEFAULT_MAX_FRACTION_SIMPLE_EDGE_MIXED_CASES,
            numAllGeneratedTests = numAllGeneratedTests
                ?: resolvedNumTests / DEFAULT_FRACTION_GENERATED_TESTS,
            numRegressionTests = numRegressionTests
                ?: resolvedNumTests / DEFAULT_FRACTION_REGRESSION_TESTS,
            maxComplexity = maxComplexity
                ?: DEFAULT_MAX_COMPLEXITY
        )
    }

    companion object {
        const val DEFAULT_NUM_TESTS = 1024
        const val DEFAULT_MAX_DISCARDS = 1024
        const val DEFAULT_MAX_FRACTION_EDGE_CASES = 16
        const val DEFAULT_MAX_FRACTION_SIMPLE_CASES = 16
        const val DEFAULT_MAX_FRACTION_SIMPLE_EDGE_MIXED_CASES = 16
        const val DEFAULT_FRACTION_GENERATED_TESTS = 2
        const val DEFAULT_FRACTION_REGRESSION_TESTS = 16
        const val DEFAULT_MAX_COMPLEXITY = 100
    }
}

fun DefaultTestRunArguments.asTestRunnerArgs(): TestRunnerArgs {
    return TestRunnerArgs(
        numTests = if (this.numTests < 0) null else this.numTests,
        maxDiscards = if (this.maxDiscards < 0) null else this.maxDiscards,
        maxOnlyEdgeCaseTests = if (this.maxOnlyEdgeCaseTests < 0) null else this.maxOnlyEdgeCaseTests,
        maxOnlySimpleCaseTests = if (this.maxOnlySimpleCaseTests < 0) null else this.maxOnlySimpleCaseTests,
        numSimpleEdgeMixedTests = if (this.numSimpleEdgeMixedTests < 0) null else this.numSimpleEdgeMixedTests,
        numAllGeneratedTests = if (this.numAllGeneratedTests < 0) null else this.numAllGeneratedTests,
        numRegressionTests = if (this.numRegressionTests < 0) null else this.numRegressionTests,
        maxComplexity = if (this.maxComplexity < 0) null else this.maxComplexity
    )
}

/**
 * An empty (all-default) [TestRunnerArgs].
 */
val defaultArgs = TestRunnerArgs()
