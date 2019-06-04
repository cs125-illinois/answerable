package examples.proxy.reference;

import edu.illinois.cs.cs125.answerable.Solution;
import edu.illinois.cs.cs125.answerable.TestOutput;
import edu.illinois.cs.cs125.answerable.Verify;
import org.junit.jupiter.api.Assertions;

@Solution
public class Widget {

    private int springs;

    @Solution
    public void positionSprings(int numSprings) {
        springs = numSprings;
    }

    public String[] getSpringPositions() {
        String[] positions = new String[springs];
        for (int i = 0; i < springs; i++) {
            positions[i] = "Spring " + (i + 1);
        }
        return positions;
    }

    @Verify
    public static void verify(TestOutput<Widget> ours, TestOutput<Widget> theirs) {
        Assertions.assertArrayEquals(ours.getReceiver().getSpringPositions(), theirs.getReceiver().getSpringPositions());
    }

}
