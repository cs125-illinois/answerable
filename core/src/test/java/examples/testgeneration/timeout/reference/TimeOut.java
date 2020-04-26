package examples.testgeneration.timeout.reference;

import edu.illinois.cs.cs125.answerable.annotations.Solution;
import edu.illinois.cs.cs125.answerable.annotations.Timeout;

public class TimeOut {
    @Solution
    @Timeout(timeout = 1000L)
    public static void doNothing() { }
}
