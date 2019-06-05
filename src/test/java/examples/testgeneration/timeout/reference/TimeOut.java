package examples.testgeneration.timeout.reference;

import edu.illinois.cs.cs125.answerable.Solution;
import edu.illinois.cs.cs125.answerable.Timeout;

@Timeout(timeout = 1000L)
public class TimeOut {
    @Solution
    public static void doNothing() { }
}
