package examples.verify;

import edu.illinois.cs.cs125.answerable.api.Generator;
import edu.illinois.cs.cs125.answerable.api.Solution;

import java.util.Random;

public class BadMethodAccessWidget {

    private int numSprings;

    public BadMethodAccessWidget(int springs) {
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

    @Generator
    public static BadMethodAccessWidget generate(int complexity, Random random) {
        BadMethodAccessWidget widget = new BadMethodAccessWidget(random.nextInt(complexity + 1));
        widget.oneMoreSpring();
        return widget;
    }

}
