package examples.verify;

import edu.illinois.cs.cs125.answerable.api.Generator;
import edu.illinois.cs.cs125.answerable.api.Solution;

import java.util.Random;

public class IncidentalInnerClassWidget {

    private int numSprings;

    public IncidentalInnerClassWidget(int springs) {
        new NamedInner().setSprings(springs);
    }

    @Solution
    public void moreSprings(int extras) {
        new Runnable() {
            @Override
            public void run() {
                numSprings += extras;
            }
        }.run();
    }

    public int getSprings() {
        return numSprings;
    }

    @Generator
    public static IncidentalInnerClassWidget generate(int complexity, Random random) {
        return new IncidentalInnerClassWidget(random.nextInt(complexity + 1));
    }

    private class NamedInner {
        private void setSprings(int springs) {
            numSprings = springs;
        }
    }

}
