package edu.illinois.cs.cs125.answerable

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class AdHoc {
    @Test
    fun test() {
        val answerable = Answerable()

        assertThrows<AnswerableVerificationException> {
            answerable.loadNewQuestion("test", examples.testing.reference.Test::class.java)
        }

        answerable.submitAndTest("test", examples.testing.reference.Test::class.java)
    }
}