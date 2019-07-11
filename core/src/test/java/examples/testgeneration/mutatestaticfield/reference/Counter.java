package examples.testgeneration.mutatestaticfield.reference;

import edu.illinois.cs.cs125.answerable.api.Solution;
import edu.illinois.cs.cs125.answerable.api.Timeout;

public class Counter {

    private static int count;

    @Solution
    @Timeout(timeout = 1000)
    public static int increment() {
        count++;
        return count;
    }

}
