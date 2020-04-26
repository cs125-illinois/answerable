package examples.proxy.reference;

import edu.illinois.cs.cs125.answerable.annotations.Generator;
import edu.illinois.cs.cs125.answerable.annotations.Helper;
import edu.illinois.cs.cs125.answerable.annotations.Solution;
import edu.illinois.cs.cs125.answerable.annotations.Verify;
import edu.illinois.cs.cs125.answerable.api.*;
import org.junit.jupiter.api.Assertions;

import java.util.Random;

public class FieldWidget {

    public int springs;

    @Helper
    private static FieldWidget lastGenerated;

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
        lastGenerated = new FieldWidget();
        return lastGenerated;
    }

}
