package edu.illinois.cs.cs125.answerable

import org.junit.jupiter.api.Test

internal class AdHoc {
    @Test
    fun test() {
        TestGenerator(examples.testing.reference.Test::class.java, examples.testing.Test::class.java).runTests(0x0403)
        // Nothing under ad-hoc testing right now
    }
}