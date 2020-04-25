package examples.testgeneration.validation.reference;

import edu.illinois.cs.cs125.answerable.DefaultTestRunArguments;
import edu.illinois.cs.cs125.answerable.Solution;

public final class Adder {

    @Solution
    @DefaultTestRunArguments(numTests = 48, maxOnlySimpleCaseTests = 1)
    public static int add(int first, int second) {
        return first + second;
    }

}
