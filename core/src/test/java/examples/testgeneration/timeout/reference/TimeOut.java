package examples.testgeneration.timeout.reference;

import edu.illinois.cs.cs125.answerable.api.Solution;
import edu.illinois.cs.cs125.answerable.api.Timeout;

public class TimeOut {
    @Solution
    @Timeout(timeout = 1000L)
    public static void doNothing() { }
}
