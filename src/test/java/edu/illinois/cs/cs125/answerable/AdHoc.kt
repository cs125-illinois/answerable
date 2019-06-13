package edu.illinois.cs.cs125.answerable

import edu.illinois.cs.cs125.answerable.api.Answerable
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class AdHoc {
    @Test
    fun test() {
        val answerable = Answerable()

        answerable.loadNewQuestion("test", examples.testing.reference.Test::class.java)
        val out = answerable.submitAndTest("test", examples.testing.Test::class.java)

        println(out.toJson())
    }
}