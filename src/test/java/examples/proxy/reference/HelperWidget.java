package examples.proxy.reference;

import edu.illinois.cs.cs125.answerable.*;
import org.junit.jupiter.api.Assertions;

import java.util.Random;

public class HelperWidget {

    private int springs;

    @Helper
    private static int numGenerated = 0;

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

    @Helper
    private static HelperWidget[] helper(String toPrint) {
        System.out.println(toPrint);
        return new HelperWidget[] { new HelperWidget() };
    }

    @Generator
    public static HelperWidget generator(int complexity, Random random) {
        numGenerated++;
        return helper(Integer.toString(numGenerated))[0];
    }

}
