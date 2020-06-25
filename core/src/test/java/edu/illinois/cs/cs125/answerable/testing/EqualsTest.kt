package edu.illinois.cs.cs125.answerable.testing

import edu.illinois.cs.cs125.answerable.TestGenerator
import edu.illinois.cs.cs125.answerable.runTestsUnsecured
import org.junit.jupiter.api.Test
import kotlin.random.Random

class EqualsTest {
    @Test
    fun testEquals() {
        TestGenerator(examples.oo.reference.Equals::class.java)
            .loadSubmission(examples.oo.reference.Equals::class.java)
            .runTestsUnsecured(Random.nextLong()).also {
                it.assertAllSucceeded()
            }
    }
}