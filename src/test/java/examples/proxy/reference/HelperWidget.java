package examples.proxy.reference;

import edu.illinois.cs.cs125.answerable.Generator;
import edu.illinois.cs.cs125.answerable.Solution;
import edu.illinois.cs.cs125.answerable.TestOutput;
import edu.illinois.cs.cs125.answerable.Verify;
import org.junit.jupiter.api.Assertions;

import java.util.Random;

@Solution
public class HelperWidget {

    private int springs;

    @Solution
    public void positionSprings(int numSprings) {
        springs = Math.abs(numSprings);
    }

    public String[] getSpringPositions() {
        String[] positions = new String[springs];
        for (int i = 0; i < springs; i++) {
            positions[i] = "Spring " + (i + 1);
        }
        return positions;
    }

    @Verify
    public static void verify(TestOutput<HelperWidget> ours, TestOutput<HelperWidget> theirs) {
        Assertions.assertArrayEquals(ours.getReceiver().getSpringPositions(), theirs.getReceiver().getSpringPositions());
    }

    private static void helper() {
        System.out.println("helper");
    }

    @Generator
    public static HelperWidget generator(int complexity, Random random) {
        helper();
        return new HelperWidget();
    }

}
