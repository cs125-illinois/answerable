package edu.illinois.cs.cs125.answerable

import org.junit.jupiter.api.Test

class EndToEndTests {
    @Test
    fun testLastTenCorrect() {
        runAnswerableTest(
            "examples.lastten.correct.LastTen",
            "examples.lastten.correct.reference.LastTen"
        ).assertAllSucceeded()
    }
}
