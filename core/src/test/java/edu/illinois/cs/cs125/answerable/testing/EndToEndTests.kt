package edu.illinois.cs.cs125.answerable.testing

import edu.illinois.cs.cs125.answerable.TestGenerator
import edu.illinois.cs.cs125.answerable.runTestsUnsecured
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.random.Random

private fun String.test(): Class<*> = Class.forName("${EndToEndTests::class.java.packageName}.fixtures.$this")

class EndToEndTests {
    @Test
    @Disabled("Will fail until multi-function problems are implemented")
    fun testGetSetEquals() {
        TestGenerator("oo.reference.GetSetEquals".test())
            .loadSubmission("oo.GetSetEquals".test())
            .runTestsUnsecured(Random.nextLong()).also {
                it.assertAllSucceeded()
            }
    }
}
