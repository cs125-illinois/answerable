package edu.illinois.cs.cs125.answerable.testing

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Random

class CaseMapTest {
    lateinit var random: Random
    @BeforeEach
    fun init() {
        random = Random(0x0403)
    }

    @Test
    fun `case map correctly generates edge cases`() {
        random = Random()
        val klass = edu.illinois.cs.cs125.answerable.testing.fixtures.GeneratorMap.MultipleFunctions::class.java
        val caseMap = klass.edgeCaseMap()
        val usesArrayCases =
            caseMap.casesForMethod(klass.getDeclaredMethod("usesArray", BooleanArray::class.java))

        fun checkCorrect() {
            assertTrue(usesArrayCases.hasNext())
            val firstCase = usesArrayCases.nextCase(random)
            val firstIsNullCase = firstCase contentEquals arrayOf<Any?>(null)
            assertTrue(firstIsNullCase || firstCase contentDeepEquals arrayOf<Any?>(booleanArrayOf()))

            val secondCase = usesArrayCases.nextCase(random)
            if (firstIsNullCase) {
                assertTrue(secondCase contentDeepEquals arrayOf<Any?>(booleanArrayOf()))
            } else {
                assertTrue(secondCase contentEquals arrayOf<Any?>(null))
            }

            assertFalse(usesArrayCases.hasNext())
        }

        checkCorrect()
        usesArrayCases.reset()
        checkCorrect()
    }
}