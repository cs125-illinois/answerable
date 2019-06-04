package examples.proxy.reference;

import edu.illinois.cs.cs125.answerable.Generator;
import edu.illinois.cs.cs125.answerable.Solution;
import edu.illinois.cs.cs125.answerable.TestOutput;
import edu.illinois.cs.cs125.answerable.Verify;
import org.junit.jupiter.api.Assertions;

import java.util.Random;

@Solution
public class GeneratedWidget {

    private int numSprings;

    public GeneratedWidget(int springs) {
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
    public static GeneratedWidget generate(int complexity, Random random) {
        return new GeneratedWidget(random.nextInt(complexity + 1));
    }

    @Verify
    public static void verify(TestOutput<GeneratedWidget> ours, TestOutput<GeneratedWidget> theirs) {
        Assertions.assertEquals(ours.getReceiver().getSprings(), theirs.getReceiver().getSprings());
    }

}
