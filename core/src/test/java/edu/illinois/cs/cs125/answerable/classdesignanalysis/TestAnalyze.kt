package edu.illinois.cs.cs125.answerable.classdesignanalysis

import com.marcinmoskala.math.combinations
import edu.illinois.cs.cs125.answerable.assertClassDesignPasses
import edu.illinois.cs.cs125.answerable.load
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

// Internal so that TestErrors can also use it to access the fixtures.
internal fun String.example(): Class<*> {
    return Class.forName("${Analyze::class.java.packageName}.fixtures.$this")
}

private fun Set<String>.correctPairs(type: String): Set<List<Class<*>>> =
    mutableSetOf<List<Class<*>>>().also { set ->
        this.forEach { set.add(listOf("$type.$it".example(), "$type.reference.$it".example())) }
        this.combinations(2).map { it.toList() }.forEach { (first, second) ->
            if (first.split("_").first() == second.split("_").first()) {
                set.add(listOf("$type.$first".example(), "$type.reference.$second".example()))
                set.add(listOf("$type.$second".example(), "$type.reference.$first".example()))
            }
        }
    }.toSet()

private fun Set<String>.incorrectPairs(type: String): Set<List<Class<*>>> =
    mutableSetOf<List<Class<*>>>().also { set ->
        this.combinations(2).map { it.toList() }.forEach { (first, second) ->
            if (first.split("_").first() != second.split("_").first()) {
                set.add(listOf("$type.$first".example(), "$type.reference.$second".example()))
                set.add(listOf("$type.$second".example(), "$type.reference.$first".example()))
            }
        }
    }.toSet()

private fun analyze(
    referenceName: String,
    submissionName: String,
    config: CDAConfig = defaultCDAConfig
): CDAResult = classDesignAnalysis(referenceName.load(), submissionName.load(), config)

internal class Analyze {
    @Test
    fun `should accept matching classes`() {
        val root = "examples.classdesign.correct"
        // ClassDesign class. @Next method should be ignored.
        val result1 = analyze("${root}1.reference.ClassDesign", "${root}1.ClassDesign")
        assertTrue(result1.allMatch)
        assertTrue(result1.errorMessages.isEmpty())
        // IterableList interface. @Next method should be ignored.
        val result2 = analyze("${root}2.reference.IterableList", "${root}2.IterableList")
        assertTrue(result2.allMatch)
        assertTrue(result2.errorMessages.isEmpty())
    }

    @Test
    fun `should check class names correctly`() {
        val klasses = setOf("Foo", "Bar")
        klasses.correctPairs("names").forEach { (first, second) ->
            first.namesMatch(second).also {
                assertTrue(it.match)
                assertEquals(it.message, "Name: $NO_ERROR_MSG")
            }
        }
        klasses.incorrectPairs("names").forEach { (first, second) ->
            first.namesMatch(second).also {
                assertFalse(it.match)
                assertNotEquals(it.message, "Name: $NO_ERROR_MSG")
            }
        }
    }

    @Test
    fun `should check class kinds correctly`() {
        val klasses = setOf("Klass", "Interfase")
        klasses.correctPairs("types").forEach { (first, second) ->
            first.kindsMatch(second).also {
                assertTrue(it.match)
            }
        }
        klasses.incorrectPairs("types").forEach { (first, second) ->
            first.kindsMatch(second).also {
                assertFalse(it.match)
            }
        }

        val matcher = "types.Klass".example().kindsMatch("types.Interfase".example())
        assertEquals(ClassKind.CLASS, matcher.reference)
        assertEquals(ClassKind.INTERFACE, matcher.submission)
    }

    @Test
    fun `should check class modifiers correctly`() {
        val klasses = setOf("Abstrict", "Finol", "StractFp")
        klasses.correctPairs("modifiers").forEach { (first, second) ->
            first.modifiersMatch(second).also {
                assertTrue(it.match)
            }
        }
        klasses.incorrectPairs("modifiers").forEach { (first, second) ->
            first.modifiersMatch(second).also {
                assertFalse(it.match)
            }
        }

        val matcher = "modifiers.Abstrict".example().modifiersMatch("modifiers.Finol".example())
        assertEquals(listOf("public", "abstract"), matcher.reference)
        assertEquals(listOf("public", "final"), matcher.submission)
    }

    @Test
    fun `should check type parameters correctly`() {
        val klasses = setOf("ET", "T")
        klasses.correctPairs("typeparameters").forEach { (first, second) ->
            first.typeParametersMatch(second).also {
                assertTrue(it.match)
            }
        }
        klasses.incorrectPairs("typeparameters").forEach { (first, second) ->
            first.typeParametersMatch(second).also {
                assertFalse(it.match)
            }
        }
        // we still need to do the ordering test, reference ET vs submission TE.
        val matcher = "typeparameters.reference.ET".example()
            .typeParametersMatch("typeparameters.TE".example())
        assertEquals(listOf("E", "T"), matcher.reference)
        assertEquals(listOf("T", "E"), matcher.submission)
    }

    @Test
    fun `should check class parents correctly`() {
        val klasses = setOf("Parent", "Child", "Parent_ExtendsObject")
        klasses.correctPairs("parents").forEach { (first, second) ->
            first.superclassesMatch(second).also {
                assertTrue(it.match)
            }
        }
        klasses.incorrectPairs("parents").forEach { (first, second) ->
            first.superclassesMatch(second).also {
                assertFalse(it.match)
            }
        }
    }

    @Test
    fun `should check class interfaces correctly`() {
        val klasses = setOf(
            "None",
            "ImplementsFoo",
            "ImplementsBar",
            "ImplementsFooBar",
            "ImplementsFoo_Again",
            "ImplementsBar_Again",
            "ImplementsFooBar_Again"
        )
        klasses.correctPairs("interfaces").forEach { (first, second) ->
            first.interfacesMatch(second).also {
                assertTrue(it.match)
            }
        }
        klasses.incorrectPairs("interfaces").forEach { (first, second) ->
            first.interfacesMatch(second).also {
                assertFalse(it.match)
            }
        }
    }

    @Test
    /**
     * this is the only test that checks public api message formatting.
     */
    fun `should check class fields correctly`() {
        val fieldsPath = "examples.classdesign.publicapi.fields"
        "$fieldsPath.reference.Simple".load().fieldsMatch(
            "$fieldsPath.Simple".load()
        ).also {
            assertTrue(it.match)
        }
        "$fieldsPath.reference.Simple".load().fieldsMatch(
            "$fieldsPath.String".load()
        ).also {
            assertFalse(it.match)
        }
    }

    @Test
    fun `should generate field names correctly`() {
        fun nameShouldBe(className: String, fieldName: String, expected: String) {
            "examples.classdesign.publicapi.fields.$className".load().declaredFields.also {
                it.find { field -> field.name == fieldName }?.answerableName().also { actual ->
                    assertEquals(expected, actual)
                } ?: fail("didn't have field")
            }
        }

        nameShouldBe("BadNames", "myInt", "public static int myInt")
        nameShouldBe("BadNames", "myString", "public String myString")
        nameShouldBe("MultipleTooMany", "extra2", "public byte extra2")
        nameShouldBe("TypeParam", "FIRST", "public final T FIRST")
    }

    @Test
    fun `should check inner classes correctly`() {
        val existenceClasses = setOf(
            "FooAndBar",
            "FooAndPrivate",
            "FooAndPrivateBad"
        )
        val correctnessClasses = setOf(
            "OneInnerMethod",
            "TwoInnerMethods" // if one inner analysis works, and outer analyses work, all inners work.
        )

        existenceClasses.correctPairs("innerclasses").forEach { (first, second) ->
            first.innerClassesMatch(second).also {
                assertTrue(it.innerClassNames.match)
            }
        }
        existenceClasses.incorrectPairs("innerclasses").forEach { (first, second) ->
            first.innerClassesMatch(second).also {
                // only one thing needs to not match
                assertFalse(it.innerClassNames.match)
            }
        }

        correctnessClasses.correctPairs("innerclasses").forEach { (first, second) ->
            first.innerClassesMatch(second).also {
                assertTrue(it.innerClassNames.match)
                assertTrue(it.recursiveResults.all { (_, result) -> result.allMatch })
            }
        }
        correctnessClasses.incorrectPairs("innerclasses").forEach { (first, second) ->
            first.innerClassesMatch(second).also {
                assertTrue(it.innerClassNames.match)
                assertFalse(it.recursiveResults.all { (_, result) -> result.allMatch })
            }
        }
    }

    @Test
    fun `should correctly check classes using expectedSubmissionName`() {
        val prefix = "examples.classdesign.differentnames"
        assertClassDesignPasses(
            "$prefix.reference.Question".load(),
            "$prefix.Submission".load(),
            CDAConfig(nameEnv = CDANameEnv("Submission"))
        )
        assertClassDesignPasses(
            "$prefix.reference.Question".load(),
            "$prefix.Incorrect".load(),
            CDAConfig(nameEnv = CDANameEnv("Incorrect"))
        )
        assertClassDesignPasses(
            "$prefix.reference.TypeNameEverywhere".load(),
            "$prefix.TNESubmission".load(),
            CDAConfig(nameEnv = CDANameEnv("TNESubmission"))
        )
    }

    @Test
    fun `should generate method names correctly`() {
        val methodsPath = "examples.classdesign.publicapi.methods"
        "$methodsPath.Constructor".load().constructors.first()
            .answerableName().also {
                assertEquals("public Constructor(int, boolean)", it)
            }
        "$methodsPath.MissingFinal".load().declaredMethods.first()
            .answerableName().also {
                assertEquals("public <T> T foo(String, T)", it)
            }
        "$methodsPath.MissingThrow".load().declaredMethods.first()
            .answerableName().also {
                assertEquals("public void foo() throws IllegalStateException", it)
            }
        "$methodsPath.Simple".load().declaredMethods.also { methods ->
            methods.find { it.name == "zero" }?.answerableName().also {
                assertEquals("public static int zero()", it)
            } ?: fail("Didn't find method")
            methods.find { it.name == "toString" }?.answerableName().also {
                assertEquals("public String toString()", it)
            } ?: fail("Didn't find method")
            methods.find { it.name == "lt" }?.answerableName().also {
                assertEquals("public boolean lt(int, int)", it)
            } ?: fail("Didn't find method")
        }
        "$methodsPath.Throws".load().declaredMethods.first()
            .answerableName().also {
                assertEquals("public void foo() throws IllegalStateException, StackOverflowError", it)
            }
        "$methodsPath.TypeParam".load().declaredMethods.first()
            .answerableName().also {
                assertEquals("public final <T> T foo(String, T)", it)
            }
        "$methodsPath.Varargs".load().declaredMethods.first()
            .answerableName().also {
                assertEquals("public void multi(int, int...)", it)
            }
    }
}
