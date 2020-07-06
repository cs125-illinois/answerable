package edu.illinois.cs.cs125.answerable.testing

import edu.illinois.cs.cs125.answerable.answerableParams
import edu.illinois.cs.cs125.answerable.publicMethods
import edu.illinois.cs.cs125.answerable.testing.fixtures.GeneratorMap.CountsInstances
import edu.illinois.cs.cs125.answerable.testing.fixtures.GeneratorMap.CountsGenerators
import edu.illinois.cs.cs125.answerable.testing.fixtures.GeneratorMap.OverridesExternalGenerator
import edu.illinois.cs.cs125.answerable.testing.fixtures.GeneratorMap.UsesExternalGenerator
import edu.illinois.cs.cs125.answerable.testing.library.Location
import examples.testgeneration.generators.reference.LabeledParamGens
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.util.Random

private fun String.test(): Class<*> =
    Class.forName("${GeneratorMapV2::class.java.packageName}.fixtures.GeneratorMap.$this")

class GeneratorMapTest {
    lateinit var random: Random
    @BeforeEach
    fun init() {
        random = Random(0x0403)
    }

    @Test
    fun `all related generators for an empty class is an empty map`() {
        assertTrue("EmptyClass".test().generatorMap().isEmpty())
    }

    @Test
    fun `all related generators for an empty class can generate receiver objects`() {
        assertTrue("EmptyClass".test().generatorMap().receiverGenerator?.canMakeInstances ?: false)
    }

    @Test
    fun `all related generators properly selects and uses a 0 argument constructor`() {
        val countsInstancesMap = "CountsInstances".test().generatorMap()
        val internalGeneratorMap = "CountsGenerators".test().generatorMap()

        assertTrue(countsInstancesMap.receiverGenerator?.canMakeInstances ?: false)
        assertTrue(internalGeneratorMap.receiverGenerator?.canMakeInstances ?: false)

        countsInstancesMap.generateReceiver(null, 0, 0, random)
        assertTrue(CountsInstances.getInstances() == 1)
        assertTrue(CountsInstances.getGeneratorCalls() == 0)
    }

    @Test
    fun `all related generators properly selects and uses an internal generator over a constructor`() {
        val countsInstancesMap = "CountsInstances".test().generatorMap()
        val internalGeneratorMap = "CountsGenerators".test().generatorMap()

        assertTrue(countsInstancesMap.receiverGenerator?.canMakeInstances ?: false)
        assertTrue(internalGeneratorMap.receiverGenerator?.canMakeInstances ?: false)

        internalGeneratorMap.generateReceiver(null, 0, 0, random)
        assertTrue(CountsGenerators.getGeneratorCalls() == 1)
        assertTrue(CountsGenerators.getNonGeneratedInstances() == 0)
    }

    @Test
    fun `all related generators correctly uses default external generators`() {
        val usesExternalGeneratorMap = "UsesExternalGenerator".test().generatorMap()
        val generatedLoc = usesExternalGeneratorMap.generateParams(
            arrayOf(Location::class.java.asGeneratorRequest()),
            0,
            random
        )[0] as Location
        assertTrue(UsesExternalGenerator.isOrigin(generatedLoc))

        val overridesExternalGeneratorMap = "OverridesExternalGenerator".test().generatorMap()
        val otherGeneratedLoc = overridesExternalGeneratorMap.generateParams(
            arrayOf(Location::class.java.asGeneratorRequest()),
            0,
            random
        )[0] as Location
        assertFalse(OverridesExternalGenerator.isOrigin(otherGeneratedLoc))
    }

    @Test
    fun `labeled parameter generators work correctly`() {
        val map = LabeledParamGens::class.java.generatorMap()
        val method = LabeledParamGens::class.java.getDeclaredMethod("testMethod", Int::class.java, Int::class.java)

        val args = map.generateParams(method.answerableParams, 10, random)
        assertEquals(0, method.invoke(null, *args))
    }

    @Test
    fun `all related generators map can make arguments for all public functions`() {
        val klass = "MultipleFunctions".test()
        val map = klass.generatorMap()

        assertEquals(3, klass.publicMethods.size)
        klass.publicMethods.forEach {
            assertTrue(map.canGenerate(it.answerableParams))
        }
    }

    @Test
    fun `arguments generated for a method can actually be passed to that method`() {
        val klass = "MultipleFunctions".test()
        val map = klass.generatorMap()

        assertEquals(3, klass.publicMethods.size)
        klass.publicMethods.forEach {
            val generator = map.generatorForMethod(it)
            for (i in 0..9) {
                val args = generator.generateParams(i * 5, random)
                // would throw an InvocationTargetException
                assertDoesNotThrow { it.invoke(null, *args) }
            }
        }
    }
}