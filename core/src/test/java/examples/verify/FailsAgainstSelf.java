package examples.verify;

import edu.illinois.cs.cs125.answerable.api.Generator;
import edu.illinois.cs.cs125.answerable.api.Solution;
import edu.illinois.cs.cs125.answerable.api.Verify;
import edu.illinois.cs.cs125.answerable.*;

import java.util.Random;
import static org.junit.jupiter.api.Assertions.fail;

public class FailsAgainstSelf {

    @Solution
    public void doNothing() { }

    @Generator
    public static FailsAgainstSelf generate(int c, Random r) {
        return new FailsAgainstSelf();
    }

    @Verify
    public static void verify(TestOutput<FailsAgainstSelf> ours, TestOutput<FailsAgainstSelf> theirs) {
        fail();
    }

}
