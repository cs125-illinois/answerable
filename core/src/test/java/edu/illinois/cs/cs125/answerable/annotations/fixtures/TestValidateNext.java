package edu.illinois.cs.cs125.answerable.annotations.fixtures;

import edu.illinois.cs.cs125.answerable.annotations.Generator;
import edu.illinois.cs.cs125.answerable.annotations.Next;

import java.util.Random;

public class TestValidateNext {
    @Next
    public static int correct1(int complexity, Random random) {
        return 0;
    }
    @Next
    public static void correct2(int complexity, Random random) {
        return;
    }

    @Next
    public int broken1(int complexity, Random random) {
        return 0;
    }
    @Next
    public static void broken2(int complexity) {
        return;
    }
}
