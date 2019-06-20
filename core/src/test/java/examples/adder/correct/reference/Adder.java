package examples.adder.correct.reference;

import edu.illinois.cs.cs125.answerable.api.EdgeCase;
import edu.illinois.cs.cs125.answerable.api.Solution;

public final class Adder {
    /*
     * Probably the easiest possible example.
     */
    @Solution
    public static int add(int first, int second) {
        return first + second;
    }

    @EdgeCase
    public static int[] intEdgeCases = new int[] { -1, 0, 1 };
}
