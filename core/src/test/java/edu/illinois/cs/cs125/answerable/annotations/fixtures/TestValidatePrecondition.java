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

    @Solution(name="1")
    public void solution1() {}
    @Precondition(name="1")
    public int broken1() {
        return 1;
    }

    @Solution(name="2")
    public static void solution2() {}
    @Precondition(name="2")
    public static boolean correct2() {
        return false;
    }

    @Solution(name="3")
    public static void solution3() {}
    @Precondition(name="3")
    public boolean broken3() {
        return true;
    }

    @Solution(name="4")
    public void solution4(int unused) {}
    @Precondition(name="4")
    public boolean correct4(int unused) {
        return false;
    }

    @Solution(name="5")
    public void solution5(int unused) {}
    @Precondition(name="5")
    public boolean broken5(boolean unused) {
        return true;
    }
}
