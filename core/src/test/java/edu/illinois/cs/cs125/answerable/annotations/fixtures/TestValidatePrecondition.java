package edu.illinois.cs.cs125.answerable.annotations.fixtures;

import edu.illinois.cs.cs125.answerable.annotations.Precondition;
import edu.illinois.cs.cs125.answerable.annotations.Solution;

public class TestValidatePrecondition {
    @Solution(name="0")
    public void solution0() {}
    @Precondition(name="0")
    public boolean correct0() {
        return true;
    }
    @Precondition(name="0")
    public int broken0() {
        return 1;
    }

    @Solution(name="1")
    public static void solution1() {}
    @Precondition(name="1")
    public static boolean correct1() {
        return false;
    }
    @Precondition(name="1")
    public boolean broken1() {
        return true;
    }

    @Solution(name="2")
    public void solution1(int unused) {}
    @Precondition(name="2")
    public boolean correct1(int unused) {
        return false;
    }
    @Precondition(name="2")
    public boolean broken1(boolean unused) {
        return true;
    }
}
