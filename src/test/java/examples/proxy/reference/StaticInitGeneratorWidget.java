package examples.proxy.reference;

import edu.illinois.cs.cs125.answerable.Generator;
import edu.illinois.cs.cs125.answerable.Helper;
import edu.illinois.cs.cs125.answerable.Next;
import edu.illinois.cs.cs125.answerable.Solution;

import java.util.Random;

public class StaticInitGeneratorWidget {

    @Helper
    private static StaticInitGeneratorWidget first = new StaticInitGeneratorWidget(8);

    private int numSprings;

    public StaticInitGeneratorWidget(int springs) {
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
    public static StaticInitGeneratorWidget generate(int complexity, Random random) {
        return first;
    }

    @Next
    public static StaticInitGeneratorWidget next(StaticInitGeneratorWidget previous, int iteration, Random random) {
        if (previous == null) previous = first;
        return new StaticInitGeneratorWidget(previous.getSprings());
    }

}
