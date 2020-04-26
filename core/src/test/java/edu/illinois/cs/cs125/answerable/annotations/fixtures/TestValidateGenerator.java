package edu.illinois.cs.cs125.answerable.annotations.fixtures;

import edu.illinois.cs.cs125.answerable.annotations.Generator;

import java.util.Random;

public class TestValidateGenerator {
    @Generator
    public static int correct1(int complexity, Random random) {
        return 0;
    }
    @Generator
    public static void correct2(int complexity, Random random) {
        return;
    }

    @Generator
    public int broken1(int complexity, Random random) {
        return 0;
    }
    @Generator
    public static void broken2(int complexity) {
        return;
    }
}
