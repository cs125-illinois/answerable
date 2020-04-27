package edu.illinois.cs.cs125.answerable.annotations.fixtures;

import edu.illinois.cs.cs125.answerable.annotations.Precondition;
import edu.illinois.cs.cs125.answerable.annotations.Solution;
import edu.illinois.cs.cs125.answerable.annotations.Verify;
import edu.illinois.cs.cs125.answerable.api.TestOutput;

import java.util.Random;

public class TestValidateVerify {
    @Solution(name="0")
    public void solution0() {}
    @Verify(name="0")
    public static void correct0(TestOutput first, TestOutput second) { }

    @Solution(name="1")
    public void solution1() {}
    @Verify(name="1")
    public static void correct1(TestOutput first, TestOutput second, Random random) { }

    @Solution(name="2")
    public void solution2() {}
    @Verify(name="2")
    public static void broken2(TestOutput first) { }
}
