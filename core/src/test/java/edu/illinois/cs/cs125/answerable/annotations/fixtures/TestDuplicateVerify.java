package edu.illinois.cs.cs125.answerable.annotations.fixtures;

import edu.illinois.cs.cs125.answerable.annotations.Precondition;
import edu.illinois.cs.cs125.answerable.annotations.Solution;
import edu.illinois.cs.cs125.answerable.annotations.Verify;
import edu.illinois.cs.cs125.answerable.api.TestOutput;

public class TestDuplicateVerify {
    @Solution
    public void solution() {}
    @Verify
    public static void broken_0(TestOutput first, TestOutput second) { }
    @Verify
    public static void broken_1(TestOutput first, TestOutput second) { }
}
