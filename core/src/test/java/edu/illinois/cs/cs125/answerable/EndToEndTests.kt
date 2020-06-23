package edu.illinois.cs.cs125.answerable

import edu.illinois.cs.cs125.answerable.api.toJson
import org.junit.jupiter.api.Test

class EndToEndTests {
    @Test
    fun testLastTenCorrect() {
        runAnswerableTest(
            "examples.lastten.correct.LastTen",
            "examples.lastten.correct.reference.LastTen"
        ).also { println(it.toJson()) }.assertAllSucceeded()
    }
}
