package edu.illinois.cs.cs125.answerable

import edu.illinois.cs.cs125.answerable.api.Service
import org.junit.jupiter.api.Test

internal class AdHoc {
    @Test
    fun test() {
        val answerable = Service(unsecuredEnvironment)

        answerable.loadNewQuestion("test", examples.testing.reference.Test::class.java)
        val out = answerable.submitAndTest("test", examples.testing.Test::class.java)

        println(out.toJson())
    }
}
