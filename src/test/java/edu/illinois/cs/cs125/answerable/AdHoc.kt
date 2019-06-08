package edu.illinois.cs.cs125.answerable

import org.junit.jupiter.api.Test

internal class AdHoc {
    @Test
    fun test() {
        val answerable = Answerable()
        answerable.loadNewQuestion("test", examples.testing.reference.Test::class.java)
        answerable.submitAndTest("test", examples.testing.reference.Test::class.java)
    }
}