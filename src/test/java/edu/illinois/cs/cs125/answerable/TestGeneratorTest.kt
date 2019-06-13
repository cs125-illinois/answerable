package edu.illinois.cs.cs125.answerable

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.random.Random

internal class TestGeneratorTest {
    @Test
    fun testMutableArguments() {
        val tg = PassedClassDesignRunner(examples.testgeneration.mutablearguments.reference.Array::class.java, examples.testgeneration.mutablearguments.Array::class.java)
        val output = tg.runTests(0x0403)
        assertTrue(output.testSteps.all { (it as ExecutedTestStep).succeeded } )
    }

    @Test
    fun testDefaultNestedPrimitiveArrayGeneration() {
        val tg = TestGenerator(examples.testgeneration.generators.defaults.reference.MultiDimensionalPrimitiveArrays::class.java)

        assertTrue(Array<IntArray>::class.java in tg.generators.keys, "Generators does not contain key `Array<Array<Int>>'.")

        assertTrue(tg.loadSubmission(examples.testgeneration.generators.defaults.MultiDemensionalPrimitiveArrays::class.java).runTests(0x0403)
            .testSteps.all { it as ExecutedTestStep; it.refOutput.threw == null && it.subOutput.threw == null }, "An error was thrown while testing nested primitive array generation")
    }

    @Test
    fun testOverrideDefaultArrayGenerator() {
        val tg = TestGenerator(examples.testgeneration.generators.reference.OverrideDefaultArray::class.java)

        assertTrue(tg.generators[Array<Array<String>>::class.java]?.gen is CustomGen, "Generators does not contain a CustomGen for `String[][]'.")
        assertEquals(1, tg.generators.size)

        tg.loadSubmission(examples.testgeneration.generators.OverrideDefaultArray::class.java).runTests(0x0403)
    }

    @Test
    fun testMissingGeneratorError() {
        val errMsg = assertThrows<AnswerableMisuseException> { PassedClassDesignRunner(
            examples.testgeneration.generators.errors.reference.MissingGenerator::class.java,
            examples.testgeneration.generators.errors.MissingGenerator::class.java
        ) }.message!!

        assertEquals("\nA generator for type `java.lang.StringBuilder' was requested, but no generator for that type was found.", errMsg)
    }

    @Test
    fun testMissingArrayComponentError() {
        val errMsg = assertThrows<AnswerableMisuseException> { PassedClassDesignRunner(
            examples.testgeneration.generators.errors.reference.MissingArrayComponent::class.java,
            examples.testgeneration.generators.errors.MissingArrayComponent::class.java
        ) }.message!!

        assertEquals("\nA generator for an array with component type `java.lang.StringBuilder' was requested, but no generator for that type was found.", errMsg)
    }

    @Test
    fun testVerifyFailsAgainstSelf() {
        val errMsg = assertThrows<AnswerableVerificationException> { TestGenerator(
            examples.verify.FailsAgainstSelf::class.java
        ) }.message!!

        assertEquals("\nTesting reference against itself failed on inputs: []", errMsg)
    }

    @Test
    fun testStandaloneVerify() {
        val tg = TestGenerator(examples.testgeneration.standaloneverify.reference.Standalone::class.java, "standalone test")
        val tr = tg.loadSubmission(examples.testgeneration.standaloneverify.Standalone::class.java)

        val out = tr.runTests(Random.nextLong())

        assertFalse(out.timedOut, "Testing timed out: ")
        assertTrue(out.testSteps.all { it as ExecutedTestStep; it.succeeded && it.refOutput.threw == null })
    }

    @Test
    fun testEdgeCases() {
        val out = TestGenerator(examples.testgeneration.reference.EdgeCases::class.java, "")
            .loadSubmission(examples.testgeneration.EdgeCases::class.java)
            .runTests(Random.nextLong())

        out.assertAllSucceeded()

    }

    @Test
    fun testReceiverEdgeCases() {
        val out = TestGenerator(examples.testgeneration.reference.ReceiverEdgeCase::class.java, "")
            .loadSubmission(examples.testgeneration.ReceiverEdgeCase::class.java)
            .runTests(Random.nextLong())

        out.assertAllSucceeded()

        assertTrue(out.testSteps.any { it as ExecutedTestStep; it.succeeded && it.refOutput.output == true })

    }

    @Test
    fun testPreconditions() {
        val out = TestGenerator(examples.testgeneration.reference.PreconditionTest::class.java)
            .loadSubmission(examples.testgeneration.PreconditionTest::class.java)
            .runTests(Random.nextLong())

        println(out.toJson())

        assertTrue(
            out.testSteps.any { it is ExecutedTestStep }
        )
        assertTrue(
            out.testSteps.any { it is DiscardedTestStep }
        )
        assertTrue(
            out.testSteps.none { (it as? ExecutedTestStep)?.refOutput?.output?.equals(false) ?: false }
        )
    }
}