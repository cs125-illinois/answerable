package examples.testgeneration.reference;

import edu.illinois.cs.cs125.answerable.annotations.EdgeCase;
import edu.illinois.cs.cs125.answerable.annotations.Generator;
import edu.illinois.cs.cs125.answerable.annotations.Solution;

import java.util.Random;

public class EdgeCases {

    @Solution
    public int doNothing(int y) {
        return y;
    }

    @EdgeCase
    public static int[] intEdgeCases = new int[] { -100000000, 100000000 };

    @Generator
    public static EdgeCases gen(int complexity, Random random) {
        return new EdgeCases();
    }

}
