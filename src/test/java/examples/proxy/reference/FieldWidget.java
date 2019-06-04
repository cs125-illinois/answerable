package examples.proxy.reference;

import edu.illinois.cs.cs125.answerable.Generator;
import edu.illinois.cs.cs125.answerable.Solution;
import edu.illinois.cs.cs125.answerable.TestOutput;
import edu.illinois.cs.cs125.answerable.Verify;
import org.junit.jupiter.api.Assertions;

import java.util.Random;

@Solution
public class FieldWidget {

    public int springs;

    @Solution
    public void addSprings(int numSprings) {
        springs = numSprings + 1;
    }

    @Verify
    public static void verify(TestOutput<FieldWidget> ours, TestOutput<FieldWidget> theirs) {
        Assertions.assertEquals(ours.getReceiver().springs, theirs.getReceiver().springs);
    }

    @Generator
    public static FieldWidget generator(int complexity, Random random) {
        return new FieldWidget();
    }

}