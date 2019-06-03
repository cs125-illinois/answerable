package examples.printer.correct.reference;

import edu.illinois.cs.cs125.answerable.Solution;
import edu.illinois.cs.cs125.answerable.TestOutput;
import static org.junit.jupiter.api.Assertions.assertEquals;
import edu.illinois.cs.cs125.answerable.Verify;

public final class Printer {
     /* Because this function is static and void answerable should determine that it must produce output. So record
     * both System.out and System.err and compare against the solution. */
    @Solution
    static void printHello() {
        System.out.println("Hello, world!");
    }

    @Verify
    static void verify(TestOutput ours, TestOutput theirs) {
        assertEquals(ours.getStdOut(), theirs.getStdOut());
    }
}
