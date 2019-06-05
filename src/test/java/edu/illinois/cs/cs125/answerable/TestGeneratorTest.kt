package edu.illinois.cs.cs125.answerable

import org.junit.jupiter.api.Test

internal class TestGeneratorTest {
    @Test
    fun testTimeOut() {
        val tg = TestGenerator(examples.testgeneration.timeout.reference.TimeOut::class.java, examples.testgeneration.timeout.TimeOut::class.java)

    }
}