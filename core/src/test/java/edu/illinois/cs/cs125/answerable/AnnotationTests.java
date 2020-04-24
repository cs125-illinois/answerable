package edu.illinois.cs.cs125.answerable;

import edu.illinois.cs.cs125.answerable.api.Solution;
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
