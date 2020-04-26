package examples.verify;

import edu.illinois.cs.cs125.answerable.annotations.Generator;
import edu.illinois.cs.cs125.answerable.annotations.Next;
import edu.illinois.cs.cs125.answerable.annotations.Solution;

import java.util.Random;

public class BadArrayParameterizedMethodAccessWidget {

    private int numSprings;

    public BadArrayParameterizedMethodAccessWidget(int springs) {
        numSprings = springs;
    }

    @Solution
    public void moreSprings(int extras) {
        numSprings += extras + 1;
    }

    public int getSprings() {
        return numSprings;
    }

    private void addSpringsFromOthers(BadArrayParameterizedMethodAccessWidget[] others) {
        for (BadArrayParameterizedMethodAccessWidget o : others) {
            numSprings += o.numSprings;
        }
    }

    @Generator
    public static BadArrayParameterizedMethodAccessWidget generate(int complexity, Random random) {
        BadArrayParameterizedMethodAccessWidget widget = new BadArrayParameterizedMethodAccessWidget(random.nextInt(complexity + 1));
        return widget;
    }

    @Next
    public static BadArrayParameterizedMethodAccessWidget next(BadArrayParameterizedMethodAccessWidget current, int iteration, Random random) {
        current.addSpringsFromOthers(new BadArrayParameterizedMethodAccessWidget[] {current});
        return current;
    }

}
