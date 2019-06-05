package edu.illinois.cs.cs125.answerable

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class TestGeneratorTest {
    @Test
    fun testMutableArguments() {
        val tg = TestGenerator(examples.testgeneration.mutablearguments.reference.Array::class.java, examples.testgeneration.mutablearguments.Array::class.java)
        val output = tg.runTests(0x0403)
        assertTrue(output.all { it.succeeded })
    }
}