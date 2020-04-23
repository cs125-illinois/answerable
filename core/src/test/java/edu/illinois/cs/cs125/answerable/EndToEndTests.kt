package edu.illinois.cs.cs125.answerable

import edu.illinois.cs.cs125.answerable.api.checkSubmission
import org.junit.jupiter.api.Test

class EndToEndTests {
    @Test
    fun testLastTenCorrect() {
        checkSubmission(
            "examples.lastten.correct.LastTen",
            "examples.lastten.correct.reference.LastTen"
        ).assertAllSucceeded()
    }
}
