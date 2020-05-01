package edu.illinois.cs.cs125.answerable.classdesignanalysis

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

// Even though we don't care much about the content of messages, these tests are still useful
// to ensure that Answerable found not only some problem, but the /right/ problem.
internal class Errors {
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
}