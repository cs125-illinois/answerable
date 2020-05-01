package edu.illinois.cs.cs125.answerable.classdesignanalysis


import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

import com.marcinmoskala.math.combinations
import edu.illinois.cs.cs125.answerable.load
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail

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
        assertTrue(analyze("${root}1.reference.ClassDesign", "${root}1.ClassDesign").allMatch)
        // IterableList interface. @Next method should be ignored.
        assertTrue(analyze("${root}2.reference.IterableList", "${root}2.IterableList").allMatch)
    }

    @Test
    fun `should check class names correctly`() {
        val klasses = setOf("Foo", "Bar")
        klasses.correctPairs("names").forEach { (first, second) ->
            first.namesMatch(second).also {
                assertTrue(it.match)
            }
        }
        klasses.incorrectPairs("names").forEach { (first, second) ->
            first.namesMatch(second).also {
                assertFalse(it.match)
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
    fun `should check class fields correctly`() {
        "examples.classdesign.publicapi.fields.reference.Simple".load().fieldsMatch(
            "examples.classdesign.publicapi.fields.Simple".load()
        ).also {
            assertTrue(it.match)
        }
        "examples.classdesign.publicapi.fields.reference.Simple".load().fieldsMatch(
            "examples.classdesign.publicapi.fields.String".load()
        ).also {
            assertFalse(it.match)
        }
    }

    @Test
    fun `should generate field names correctly`() {
        fun nameShouldBe(className: String, fieldName: String, expected: String) {
            "examples.classdesign.publicapi.fields.$className".load().declaredFields.also {
                it.find { it.name == fieldName }?.answerableName().also { actual ->
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
    fun `should generate method names correctly`() {
        "examples.classdesign.publicapi.methods.Constructor".load().constructors.first()
            .answerableName().also {
                assertEquals("public Constructor(int, boolean)", it)
            }
        "examples.classdesign.publicapi.methods.MissingFinal".load().declaredMethods.first()
            .answerableName().also {
                assertEquals("public <T> T foo(String, T)", it)
            }
        "examples.classdesign.publicapi.methods.MissingThrow".load().declaredMethods.first()
            .answerableName().also {
                assertEquals("public void foo() throws IllegalStateException", it)
            }
        "examples.classdesign.publicapi.methods.Simple".load().declaredMethods.also { methods ->
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
        "examples.classdesign.publicapi.methods.Throws".load().declaredMethods.first()
            .answerableName().also {
                assertEquals("public void foo() throws IllegalStateException, StackOverflowError", it)
            }
        "examples.classdesign.publicapi.methods.TypeParam".load().declaredMethods.first()
            .answerableName().also {
                assertEquals("public final <T> T foo(String, T)", it)
            }
        "examples.classdesign.publicapi.methods.Varargs".load().declaredMethods.first()
            .answerableName().also {
                assertEquals("public void multi(int, int...)", it)
            }
    }
}
