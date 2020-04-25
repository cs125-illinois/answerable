package examples.testgeneration.reference;

import edu.illinois.cs.cs125.answerable.Generator;
import edu.illinois.cs.cs125.answerable.Precondition;
import edu.illinois.cs.cs125.answerable.Solution;
import edu.illinois.cs.cs125.answerable.Verify;
import edu.illinois.cs.cs125.answerable.api.*;

import java.util.Random;

public class PreconditionTest {

    @Solution
    public boolean test(int x, int y) {
        return x != y;
    }

    @Precondition
    public boolean precondition(int x, int y) {
        return x != y;
    }

    @Verify
    public static void verifyPrecondition(TestOutput<PreconditionTest> ours, TestOutput<PreconditionTest> theirs) {
        if (!((Boolean) ours.getOutput())) {
            throw new IllegalStateException();
        }
    }

    @Generator
    public static int genInt(int complexity, Random r) {
        return r.nextInt(5);
    }

    @Generator
    public static PreconditionTest gen(int complexity, Random r) {
        return new PreconditionTest();
    }

}
