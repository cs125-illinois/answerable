package edu.illinois.cs.cs125.answerable.classdesignanalysis

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

import com.marcinmoskala.math.combinations
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail

internal class Analyze {
    @Test
    fun `should check class names correctly`() {
        "examples.classdesign.correct1.reference.ClassDesign".load().namesMatch(
            "examples.classdesign.correct1.ClassDesign".load()
        ).also {
            assertTrue(it.matched)
        }
        "examples.classdesign.correct1.reference.ClassDesign".load().namesMatch(
            "examples.classdesign.correct2.IterableList".load()
        ).also {
            assertFalse(it.matched)
        }
    }

    @Test
    fun `should check class types correctly`() {
        "examples.classdesign.correct1.reference.ClassDesign".load().typesMatch(
            "examples.classdesign.correct1.ClassDesign".load()
        ).also {
            assertTrue(it.matched)
        }
        "examples.classdesign.correct2.reference.IterableList".load().typesMatch(
            "examples.classdesign.correct2.IterableList".load()
        ).also {
            assertTrue(it.matched)
        }
        "examples.classdesign.correct1.reference.ClassDesign".load().typesMatch(
            "examples.classdesign.correct2.IterableList".load()
        ).also {
            assertFalse(it.matched)
        }
        "examples.classdesign.correct2.reference.IterableList".load().typesMatch(
            "examples.classdesign.correct1.ClassDesign".load()
        ).also {
            assertFalse(it.matched)
        }
        "examples.classdesign.statusmismatch.reference.Question".load().typesMatch(
            "examples.classdesign.statusmismatch.Question".load()
        ).also {
            assertFalse(it.matched)
        }
    }

    @Test
    fun `should check class modifiers correctly`() {
        "examples.classdesign.correct1.reference.ClassDesign".load().modifiersMatch(
            "examples.classdesign.correct1.ClassDesign".load()
        ).also {
            assertTrue(it.matched)
        }
        "examples.classdesign.modifiers.reference.Final".load().modifiersMatch(
            "examples.classdesign.modifiers.Abstract".load()
        ).also {
            assertFalse(it.matched)
        }
    }

    @Test
    fun `should check class parents correctly`() {
        "examples.classdesign.correct1.reference.ClassDesign".load().parentsMatch(
            "examples.classdesign.correct1.ClassDesign".load()
        ).also {
            assertTrue(it.matched)
        }
        setOf(
            "examples.classdesign.superclassmismatch.classes.reference.OnlyExt",
            "examples.classdesign.superclassmismatch.classes.OnlyExtNone",
            "examples.classdesign.superclassmismatch.classes.OnlyExtWrong"
        ).map { it.load() }.toSet().combinations(2).map { it.toList() }.forEach { (first, second) ->
            first.parentsMatch(second).also {
                assertFalse(it.matched)
            }
        }
    }

    @Test
    fun `should check class interfaces correctly`() {
        "examples.classdesign.superclassmismatch.interfaces.reference.None".load().interfacesMatch(
            "examples.classdesign.superclassmismatch.interfaces.None".load()
        ).also {
            assertTrue(it.matched)
        }
        listOf("Multiple", "One").forEach {
            "examples.classdesign.superclassmismatch.interfaces.reference.$it".load().interfacesMatch(
                "examples.classdesign.superclassmismatch.interfaces.$it".load()
            ).also { classDesignMatch ->
                assertFalse(classDesignMatch.matched)
            }
        }
        setOf("Multiple", "One", "None").map {
            "examples.classdesign.superclassmismatch.interfaces.$it".load()
        }.toSet().combinations(2).map { it.toList() }.forEach { (first, second) ->
            first.interfacesMatch(second).also {
                assertFalse(it.matched)
            }
        }
    }

    @Test
    fun `should check class fields correctly`() {
        "examples.classdesign.publicapi.fields.reference.Simple".load().fieldsMatch(
            "examples.classdesign.publicapi.fields.Simple".load()
        ).also {
            assertTrue(it.matched)
        }
        "examples.classdesign.publicapi.fields.reference.Simple".load().fieldsMatch(
            "examples.classdesign.publicapi.fields.String".load()
        ).also {
            assertFalse(it.matched)
        }
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