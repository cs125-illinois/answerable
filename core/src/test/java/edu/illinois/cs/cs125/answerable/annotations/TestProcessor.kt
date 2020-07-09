package edu.illinois.cs.cs125.answerable.annotations

import edu.illinois.cs.cs125.answerable.assertClassDesignPasses
import edu.illinois.cs.cs125.answerable.classdesignanalysis.CDAConfig
import edu.illinois.cs.cs125.answerable.classdesignanalysis.CDANameEnv
import edu.illinois.cs.cs125.answerable.classdesignanalysis.message
import edu.illinois.cs.cs125.answerable.classdesignanalysis.methodsMatch
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.reflect.Modifier

private fun String.test() =
    Class.forName("${AnswerableInterfaceProcessor::class.java.packageName}.fixtures.processor.$this")

// We use reflection to assess the shape of the interfaces because otherwise processor errors would cause compilation
// failures instead of test failures, which would be harder to track down.
class TestProcessor {
    private fun assertInterfaceShape(className: String) {
        val origClass = className.test()
        val ifaceClass = "I$className".test()
        val nameEnv = CDANameEnv(expectedSubmissionName = "I$className")
        assertTrue(ifaceClass.isInterface, "Generated class for $className should be an interface")
        // checks name, superinterfaces, type parameters
        // inner classes are not currently supported at all
        assertClassDesignPasses(
            origClass,
            ifaceClass,
            CDAConfig(
                checkConstructors = false,
                checkSuperclasses = false,
                checkKind = false,
                checkModifiers = false,
                checkFields = false,
                checkMethods = false,
                checkInnerClasses = false,
                nameEnv = nameEnv
            )
        )

        val classModifiers = Modifier.toString(origClass.modifiers).split(" ")
        val ifaceModifiers = Modifier.toString(ifaceClass.modifiers).split(" ")
        assertEquals(classModifiers, ifaceModifiers.filter { it != "abstract" && it != "interface" })

        val methodMatcher = origClass.methodsMatch(ifaceClass, nameEnv = nameEnv)
        val methodMatcherWithAdjustedModifiers = methodMatcher.copy(
            reference = methodMatcher.reference.map {
                it.copy(modifiers = it.modifiers.filter { modifier -> modifier != "static" })
            }.sortedBy { it.answerableName },
            submission = methodMatcher.submission.map {
                it.copy(
                    modifiers = it.modifiers.filter { modifier ->
                        modifier != "abstract"
                    },
                    isDefault = false
                )
            }.sortedBy { it.answerableName }
        )
        assertTrue(methodMatcherWithAdjustedModifiers.match) { methodMatcherWithAdjustedModifiers.message }
    }

    @Test
    fun `simple processor case`() {
        assertInterfaceShape("Simple")
    }

    @Test
    fun `self-referential return case`() {
        assertInterfaceShape("StaticFactory")
    }

    @Test
    fun `complex self-referential case`() {
        assertInterfaceShape("IsComparable")
    }
}