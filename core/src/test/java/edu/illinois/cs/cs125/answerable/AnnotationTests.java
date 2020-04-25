package edu.illinois.cs.cs125.answerable;

import org.junit.jupiter.api.Test;

public class AnnotationTests {
    @Test
    void testAnnotationImport() {
        Object unused = new Object() {
            @Solution
            void testMethod() { }
        };
    }
}
