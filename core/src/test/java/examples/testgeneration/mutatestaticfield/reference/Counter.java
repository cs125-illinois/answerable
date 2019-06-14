package examples.testgeneration.mutatestaticfield.reference;

import edu.illinois.cs.cs125.answerable.Solution;

public class Counter {

    private static int count;

    @Solution
    public static int increment() {
        count++;
        return count;
    }

}
