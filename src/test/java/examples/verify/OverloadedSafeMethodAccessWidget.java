package examples.verify;

import edu.illinois.cs.cs125.answerable.Generator;
import edu.illinois.cs.cs125.answerable.Solution;
import edu.illinois.cs.cs125.answerable.TestOutput;
import edu.illinois.cs.cs125.answerable.Verify;
import examples.proxy.reference.GeneratedWidget;
import org.junit.jupiter.api.Assertions;

import java.util.Random;

public class OverloadedSafeMethodAccessWidget {

    private int numSprings;

    public OverloadedSafeMethodAccessWidget(int springs) {
        numSprings = springs;
    }

    @Solution
    public void moreSprings(int extras) {
        oneMoreSpring();
        numSprings += extras;
    }

    public int getSprings() {
        return numSprings;
    }

    private int oneMoreSpring() {
        numSprings++;
        return numSprings;
    }

    public int oneMoreSpring(boolean returnZero) {
        int newSprings = oneMoreSpring();
        return returnZero ? 0 : newSprings;
    }

    @Generator
    public static OverloadedSafeMethodAccessWidget generate(int complexity, Random random) {
        OverloadedSafeMethodAccessWidget widget = new OverloadedSafeMethodAccessWidget(random.nextInt(complexity + 1));
        widget.oneMoreSpring(true);
        return widget;
    }

    @Verify
    public static void verify(TestOutput<GeneratedWidget> ours, TestOutput<GeneratedWidget> theirs) {
        Assertions.assertEquals(ours.getReceiver().getSprings(), theirs.getReceiver().getSprings());
    }

}
