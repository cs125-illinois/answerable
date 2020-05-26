package edu.illinois.cs.cs125.answerable.classdesignanalysis

import edu.illinois.cs.cs125.answerable.load
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

// Even though we don't care much about the content of messages, these tests are still useful
// to ensure that Answerable found not only some problem, but the /right/ problem.
internal class TestErrors {
    @Test
    fun `should correctly identify issues with class names`() {
        Assertions.assertEquals(
            "Class name mismatch;\nExpected: Foo\nFound:    Bar",
            "names.Foo".example().namesMatch("names.Bar".example()).message
        )
        Assertions.assertEquals(
            "Name: All good!",
            "names.Foo".example().namesMatch("names.Foo".example()).message
        )
    }

    @Test
    fun `should correctly identify issues with superclasses`() {
        val matcher = "parents.Parent".example().superclassesMatch("parents.Child".example())
        Assertions.assertNull(matcher.reference)
        Assertions.assertEquals("Parent", matcher.submission)
        Assertions.assertEquals(
            "Superclass mismatch;\nExpected: No class to be extended\nFound:    extends Parent",
            matcher.message
        )
    }

    @Test
    fun `should correctly identify issues with interfaces`() {
        val matcher = "interfaces.ImplementsFoo".example()
            .interfacesMatch("interfaces.ImplementsFooBar".example())
        Assertions.assertEquals(listOf("Foo"), matcher.reference)
        Assertions.assertEquals(listOf("Bar", "Foo"), matcher.submission)
        Assertions.assertEquals(
            "Interface mismatch;\nExpected: implements Foo\nFound:    implements Bar, Foo",
            matcher.message
        )
    }

    @Test
    fun `should correctly identify issues with fields`() {
        val fieldsPath = "examples.classdesign.publicapi.fields"
        val matcher = "$fieldsPath.reference.Simple".load()
            .fieldsMatch("$fieldsPath.OneTooMany".load())
        Assertions.assertEquals(
            listOf("public String s", "public static int a"),
            matcher.reference.map { it.answerableName })
        Assertions.assertEquals(
            listOf("public String s", "public boolean extra", "public static int a"),
            matcher.submission.map { it.answerableName }
        )
        Assertions.assertEquals("Found an unexpected public field:\n  public boolean extra", matcher.message)
    }
}
