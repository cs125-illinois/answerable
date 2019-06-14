package examples.verify;

import edu.illinois.cs.cs125.answerable.Generator;
import edu.illinois.cs.cs125.answerable.Solution;
import edu.illinois.cs.cs125.answerable.TestOutput;
import edu.illinois.cs.cs125.answerable.Verify;
import examples.proxy.reference.GeneratedWidget;
import org.junit.jupiter.api.Assertions;

import java.util.Random;

public class BadFieldAccessWidget {

    private int numSprings;

    public BadFieldAccessWidget(int springs) {
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
    public static BadFieldAccessWidget generate(int complexity, Random random) {
        BadFieldAccessWidget widget = new BadFieldAccessWidget(random.nextInt(complexity + 1));
        widget.numSprings++;
        return widget;
    }

}
