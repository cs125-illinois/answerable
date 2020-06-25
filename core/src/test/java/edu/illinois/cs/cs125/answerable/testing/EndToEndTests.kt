package edu.illinois.cs.cs125.answerable.testing

import edu.illinois.cs.cs125.answerable.TestGenerator
import edu.illinois.cs.cs125.answerable.runTestsUnsecured
import org.junit.jupiter.api.Test
import kotlin.random.Random

private fun String.load(): Class<*> = Class.forName("${EndToEndTests::class.java.packageName}.fixtures.$this")

class EndToEndTests {
    @Test
    fun testGetSetEquals() {
        TestGenerator("oo.reference.GetSetEquals".load())
            .loadSubmission("oo.GetSetEquals".load())
            .runTestsUnsecured(Random.nextLong()).also {
                it.assertAllSucceeded()
            }
    }
}
