package edu.illinois.cs.cs125.answerable

import examples.testgeneration.validation.reference.Adder
import examples.testgeneration.validation.reference.ArgsOnStandaloneVerify
import kotlin.random.Random
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class TestGeneratorTest {

    @Test
    fun testMutableArguments() {
        val tg = PassedClassDesignRunner(examples.testgeneration.mutablearguments.reference.Array::class.java, examples.testgeneration.mutablearguments.Array::class.java)
        val output = tg.runTestsUnsecured(0x0403)
        assertTrue(output.testSteps.all { (it as ExecutedTestStep).succeeded })
    }

    @Test
    fun testDefaultNestedPrimitiveArrayGeneration() {
        val tg = TestGenerator(examples.testgeneration.generators.defaults.reference.MultiDimensionalPrimitiveArrays::class.java)

        assertTrue(Pair(Array<IntArray>::class.java, null) in tg.generators.keys, "Generators does not contain key `Array<Array<Int>>'.")

        assertTrue(tg.loadSubmission(examples.testgeneration.generators.defaults.MultiDimensionalPrimitiveArrays::class.java).runTestsUnsecured(0x0403)
            .testSteps.all { it as ExecutedTestStep; it.refOutput.threw == null && it.subOutput.threw == null }, "An error was thrown while testing nested primitive array generation")
    }

    @Test
    fun testOverrideDefaultArrayGenerator() {
        val tg = TestGenerator(examples.testgeneration.generators.reference.OverrideDefaultArray::class.java)

        assertTrue(tg.generators[Pair(Array<Array<String>>::class.java, null)]?.gen is CustomGen, "Generators does not contain a CustomGen for `String[][]'.")
        assertEquals(1, tg.generators.size)

        tg.loadSubmission(examples.testgeneration.generators.OverrideDefaultArray::class.java).runTestsUnsecured(0x0403)
    }

    @Test
    fun testWildcardTypeSpecialization() {
        val tg = TestGenerator(examples.testgeneration.generators.reference.WildcardTypes::class.java)
        tg.loadSubmission(examples.testgeneration.generators.WildcardTypes::class.java).runTestsUnsecured(0x0403)
    }

    @Test
    fun testDefaultConstructor() {
        val out = TestGenerator(examples.testgeneration.generators.defaults.reference.DefaultCtor::class.java)
                .loadSubmission(examples.testgeneration.generators.defaults.DefaultCtor::class.java).runTestsUnsecured(0x0403)
        out.assertAllSucceeded()
    }

    @Test
    fun testOverrideDefaultConstructor() {
        val out = TestGenerator(examples.testgeneration.generators.reference.OverrideDefaultCtor::class.java)
                .loadSubmission(examples.testgeneration.generators.OverrideDefaultCtor::class.java).runTestsUnsecured(0x0403)
        out.assertAllSucceeded()
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
    fun testMissingReceiverGeneratorError() {
        val err = assertThrows<AnswerableMisuseException> { PassedClassDesignRunner(
                examples.testgeneration.generators.errors.reference.MissingReceiverGenerator::class.java,
                examples.testgeneration.generators.errors.MissingReceiverGenerator::class.java
        ) }

        assertEquals("\nThe reference solution must provide either an @Generator or an @Next method if @Solution is not static and no zero-argument constructor is accessible.",
                err.message)
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
    fun testStaticTakesInstanceError() {
        val errMsg = assertThrows<AnswerableMisuseException> { PassedClassDesignRunner(
                examples.testgeneration.generators.reference.StaticTakesInstance::class.java,
                examples.testgeneration.generators.StaticTakesInstance::class.java) }.message

        assertEquals(
            "\nA generator for type `examples.testgeneration.generators.reference.StaticTakesInstance' " +
                "was requested, but no generator for that type was found.", errMsg)
    }

    @Test
    fun testDefaultCtorNeededAsArgumentError() {
        val errMsg = assertThrows<AnswerableMisuseException> { PassedClassDesignRunner(
                examples.testgeneration.generators.reference.DefaultCtorClassAsParam::class.java,
                examples.testgeneration.generators.DefaultCtorClassAsParam::class.java) }.message

        assertEquals(
            "\nA generator for type `examples.testgeneration.generators.reference.DefaultCtorClassAsParam' " +
                "was requested, but no generator for that type was found.", errMsg)
    }

    @Test
    fun testStandaloneVerify() {
        val tg = TestGenerator(
            examples.testgeneration.standaloneverify.reference.Standalone::class.java, "standalone test"
        )
        val tr = tg.loadSubmission(examples.testgeneration.standaloneverify.Standalone::class.java)

        val out = tr.runTestsUnsecured(Random.nextLong())

        assertFalse(out.timedOut, "Testing timed out: ")
        assertTrue(out.testSteps.all { it as ExecutedTestStep; it.succeeded && it.refOutput.threw == null })
    }

    @Test
    fun testEdgeCases() {
        val out = TestGenerator(examples.testgeneration.reference.EdgeCases::class.java)
            .loadSubmission(examples.testgeneration.EdgeCases::class.java)
            .runTestsUnsecured(Random.nextLong())

        out.assertAllSucceeded()
    }

    @Test
    fun testReceiverEdgeCases() {
        val out = TestGenerator(examples.testgeneration.reference.ReceiverEdgeCase::class.java)
            .loadSubmission(examples.testgeneration.ReceiverEdgeCase::class.java)
            .runTestsUnsecured(Random.nextLong())

        out.assertAllSucceeded()

        assertTrue(out.testSteps.any { it as ExecutedTestStep; it.succeeded && it.refOutput.output?.value == "true" })
    }

    @Test
    fun testPreconditions() {
        val out = TestGenerator(examples.testgeneration.reference.PreconditionTest::class.java)
            .loadSubmission(examples.testgeneration.PreconditionTest::class.java)
            .runTestsUnsecured(Random.nextLong())

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

    @Test
    fun testMutatedStaticField() {
        val generator = TestGenerator(examples.testgeneration.mutatestaticfield.reference.Counter::class.java)
        val firstOut = generator.loadSubmission(examples.testgeneration.mutatestaticfield.Counter::class.java).runTestsUnsecured(0x0403)
        firstOut.assertAllSucceeded()
        val secondOut = generator.loadSubmission(examples.testgeneration.mutatestaticfield.another.Counter::class.java).runTestsUnsecured(403)
        secondOut.assertAllSucceeded()
    }

    @Test
    fun testMutatedStaticFieldWithTimeout() {
        val generator = TestGenerator(examples.testgeneration.mutatestaticfield.reference.Counter::class.java)
        val firstOut = generator.loadSubmission(examples.testgeneration.mutatestaticfield.timesout.Counter::class.java).runTestsUnsecured(0x0403)
        assertTrue(firstOut.timedOut)
        val secondOut = generator.loadSubmission(examples.testgeneration.mutatestaticfield.Counter::class.java).runTestsUnsecured(403)
        secondOut.assertAllSucceeded()
    }

    @Test
    fun testIntArrayParameter() {
        val out = TestGenerator(examples.testgeneration.arrays.reference.IntArrayParameter::class.java)
                .loadSubmission(examples.testgeneration.arrays.IntArrayParameter::class.java)
                .runTestsUnsecured(Random.nextLong())

        out.assertAllSucceeded()
    }

    @Test
    fun testIntArrayArrayParameter() {
        val out = TestGenerator(examples.testgeneration.arrays.reference.IntArrayArrayParameter::class.java)
                .loadSubmission(examples.testgeneration.arrays.IntArrayArrayParameter::class.java)
                .runTestsUnsecured(Random.nextLong())

        out.assertAllSucceeded()
    }

    @Test
    fun testOverrideDefaultGenerator() {
        val out = TestGenerator(examples.testgeneration.generators.reference.LabeledParamGens::class.java)
            .loadSubmission(examples.testgeneration.generators.LabeledParamGens::class.java)
            .runTestsUnsecured(Random.nextLong())

        out.assertAllSucceeded()
    }

    @Test
    fun testRunnerArgsOverriding() {
        val seed = Random.nextLong()
        val gen = TestGenerator(examples.adder.correct.reference.Adder::class.java, testRunnerArgs = TestRunnerArgs(numTests = 64))
        val defaultOut = gen.loadSubmission(examples.adder.correct.Adder::class.java).runTestsUnsecured(seed)
        assertEquals(64, defaultOut.numTests)
        val submission = gen.loadSubmission(examples.adder.correct.Adder::class.java, testRunnerArgs = TestRunnerArgs(numTests = 48))
        val submissionOut = submission.runTestsUnsecured(seed)
        assertEquals(48, submissionOut.numTests)
        val runOut = submission.runTestsUnsecured(seed, testRunnerArgs = TestRunnerArgs(numTests = 32))
        assertEquals(32, runOut.numTests)
        val newRunOut = gen.loadSubmission(examples.adder.correct.Adder::class.java)
                .runTestsUnsecured(seed, testRunnerArgs = TestRunnerArgs(numTests = 16))
        assertEquals(16, newRunOut.numTests)
    }

    @Test
    fun testRunnerArgsMerging() {
        val seed = Random.nextLong()
        val out = TestGenerator(examples.adder.correct.reference.Adder::class.java, testRunnerArgs = TestRunnerArgs(numTests = 128))
                .loadSubmission(examples.adder.correct.Adder::class.java, testRunnerArgs = TestRunnerArgs(maxOnlySimpleCaseTests = 1))
                .runTestsUnsecured(seed, testRunnerArgs = TestRunnerArgs(maxOnlyEdgeCaseTests = 2))
        assertEquals(128, out.numTests)
        assertEquals(1, out.numSimpleCaseTests)
        assertEquals(2, out.numEdgeCaseTests)
    }

    @Test
    fun testAnnotationRunnerArgs() {
        val out = TestGenerator(Adder::class.java, testRunnerArgs = TestRunnerArgs(numTests = 64))
                .loadSubmission(examples.adder.correct.Adder::class.java).runTestsUnsecured(0x0403)
        assertEquals(64, out.numTests)
        assertEquals(1, out.numSimpleCaseTests)
    }

    @Test
    fun testArgsAnnotationOnStandaloneVerify() {
        val out = TestGenerator(ArgsOnStandaloneVerify::class.java)
                .loadSubmission(examples.testgeneration.validation.ArgsOnStandaloneVerify::class.java).runTestsUnsecured(0x0403)
        assertEquals(96, out.numTests)
    }

    @Test
    fun testVerifyWithRandom() {
        val out = TestGenerator(examples.testgeneration.reference.VerifyWithRandom::class.java)
            .loadSubmission(examples.testgeneration.VerifyWithRandom::class.java)
            .runTestsUnsecured(Random.nextLong())

        out.assertAllSucceeded()
    }

    @Test
    fun testLastTen() {
        val out = TestGenerator(examples.lastten.correct.reference.LastTen::class.java)
                .loadSubmission(examples.lastten.correct.LastTen::class.java)
                .runTestsUnsecured(Random.nextLong())
        out.testSteps.filterIsInstance<ExecutedTestStep>().filter { !it.succeeded }.forEach { println(it.assertErr) }
        assertTrue(out.testSteps.filterIsInstance<ExecutedTestStep>().all { it.succeeded })
    }

    @Test
    fun testPersistentStaticLastTen() {
        assertClassDesignPasses(examples.lastten.correct.reference.LastTen::class.java,
                examples.lastten.persistentstatic.LastTen::class.java)
        val out = TestGenerator(examples.lastten.correct.reference.LastTen::class.java)
                .loadSubmission(examples.lastten.persistentstatic.LastTen::class.java)
                .runTestsUnsecured(Random.nextLong())
        assertTrue(out.testSteps.filterIsInstance<ExecutedTestStep>().any { !it.succeeded })
    }

    @Test
    fun testReinitializedStaticLastTen() {
        assertClassDesignPasses(examples.lastten.correct.reference.LastTen::class.java,
                examples.lastten.reinitstatic.LastTen::class.java)
        val out = TestGenerator(examples.lastten.correct.reference.LastTen::class.java)
                .loadSubmission(examples.lastten.reinitstatic.LastTen::class.java)
                .runTestsUnsecured(Random.nextLong())
        assertTrue(out.testSteps.filterIsInstance<ExecutedTestStep>().any { !it.succeeded })
    }

    @Test
    fun testGenerationFailed() {
        // TODO: This will fail until the GENERATION_FAILED behavior is assessed
        val result = PassedClassDesignRunner(examples.proxy.reference.Widget::class.java,
            examples.proxy.ExplodingCtorWidget::class.java).runTestsUnsecured(0x0403)
        assertTrue(result.testSteps.filterIsInstance<ExecutedTestStep>().any { !it.succeeded })
    }

}
