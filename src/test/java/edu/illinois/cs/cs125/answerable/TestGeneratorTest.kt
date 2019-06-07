package edu.illinois.cs.cs125.answerable

import examples.testgeneration.generators.defaults.MultiDemensionalPrimitiveArrays
import examples.testgeneration.generators.defaults.reference.MultiDimensionalPrimitiveArrays
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class TestGeneratorTest {
    @Test
    fun testMutableArguments() {
        val tg = TestRunner(examples.testgeneration.mutablearguments.reference.Array::class.java, examples.testgeneration.mutablearguments.Array::class.java)
        val output = tg.runTests(0x0403)
        assertTrue(output.all { it.succeeded })
    }

    @Test
    fun testDefaultNestedPrimitiveArrayGeneration() {
        val tg = TestGenerator(MultiDimensionalPrimitiveArrays::class.java)

        assertTrue(Array<IntArray>::class.java in tg.generators.keys, "Generators does not contain key `Array<Array<Int>>'.")

        tg.loadSubmission(examples.testgeneration.generators.defaults.MultiDemensionalPrimitiveArrays::class.java).runTests(0x0403)
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
        val errMsg = assertThrows<AnswerableMisuseException> { TestRunner(
            examples.testgeneration.generators.errors.reference.MissingGenerator::class.java,
            examples.testgeneration.generators.errors.MissingGenerator::class.java
        ) }.message!!

        assertEquals("\nA generator for type `java.lang.StringBuilder' was requested, but no generator for that type was found.", errMsg)
    }

    @Test
    fun testMissingArrayComponentError() {
        val errMsg = assertThrows<AnswerableMisuseException> { TestRunner(
            examples.testgeneration.generators.errors.reference.MissingArrayComponent::class.java,
            examples.testgeneration.generators.errors.MissingArrayComponent::class.java
        ) }.message!!

        assertEquals("\nA generator for an array with component type `java.lang.StringBuilder' was requested, but no generator for that type was found.", errMsg)
    }
}