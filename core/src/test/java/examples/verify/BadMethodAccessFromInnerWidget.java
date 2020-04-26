package examples.verify;

import edu.illinois.cs.cs125.answerable.annotations.Generator;
import edu.illinois.cs.cs125.answerable.annotations.Solution;

import java.util.Random;

public class BadMethodAccessFromInnerWidget {

    private int numSprings;

    public BadMethodAccessFromInnerWidget(int springs) {
        numSprings = springs;
    }

    @Solution
    public void moreSprings(int extras) {
        numSprings += extras + 1;
    }

    public int getSprings() {
        return numSprings;
    }

    private void oneMoreSpring() {
        moreSprings(0);
    }

    @Generator
    public static BadMethodAccessFromInnerWidget generate(int complexity, Random random) {
        BadMethodAccessFromInnerWidget widget = new BadMethodAccessFromInnerWidget(random.nextInt(complexity + 1));
        new Runnable() {
            @Override
            public void run() {
                new Runnable() {
                    @Override
                    public void run() {
                        widget.oneMoreSpring();
                    }
                }.run();
            }
        }.run();
        return widget;
    }

}
