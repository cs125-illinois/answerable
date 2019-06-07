package examples.verify;

import edu.illinois.cs.cs125.answerable.Generator;
import edu.illinois.cs.cs125.answerable.Solution;
import edu.illinois.cs.cs125.answerable.TestOutput;
import edu.illinois.cs.cs125.answerable.Verify;
import examples.proxy.reference.GeneratedWidget;
import org.junit.jupiter.api.Assertions;

import java.util.Random;

public class BadFieldAccessFromInnerWidget {

    private int numSprings;

    public BadFieldAccessFromInnerWidget(int springs) {
        numSprings = springs;
    }

    @Solution
    public void moreSprings(int extras) {
        numSprings += extras + 1;
    }

    public int getSprings() {
        return numSprings;
    }

    @Generator
    public static BadFieldAccessFromInnerWidget generate(int complexity, Random random) {
        BadFieldAccessFromInnerWidget widget = new BadFieldAccessFromInnerWidget(random.nextInt(complexity + 1));
        new Runnable() {
            @Override
            public void run() {
                widget.numSprings++;
            }
        }.run();
        return widget;
    }

    @Verify
    public static void verify(TestOutput<GeneratedWidget> ours, TestOutput<GeneratedWidget> theirs) {
        Assertions.assertEquals(ours.getReceiver().getSprings(), theirs.getReceiver().getSprings());
    }

}
