package examples.testgeneration.reference;

import edu.illinois.cs.cs125.answerable.api.TestOutput;
import edu.illinois.cs.cs125.answerable.Verify;

import org.junit.jupiter.api.Assertions;

import java.util.Random;

public class VerifyWithRandom {

    public int sum(int a, int b) {
        return a + b;
    }

    @Verify(standalone = true)
    private static void verify(TestOutput<VerifyWithRandom> ours, TestOutput<VerifyWithRandom> theirs, Random r) {
        int a = r.nextInt();
        int b = r.nextInt();

        int outo = ours.getReceiver().sum(a, b);
        int outt = theirs.getReceiver().sum(a, b);

        Assertions.assertEquals(outo, outt);
    }

}
