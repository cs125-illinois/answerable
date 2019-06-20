package examples.verify;

import edu.illinois.cs.cs125.answerable.api.Generator;
import edu.illinois.cs.cs125.answerable.api.Next;
import edu.illinois.cs.cs125.answerable.api.Solution;

import java.util.Random;

public class BadPrimitiveArrayParameterizedMethodAccessWidget {

    private int numSprings;

    public BadPrimitiveArrayParameterizedMethodAccessWidget(int springs) {
        numSprings = springs;
    }

    @Solution
    public void moreSprings(int extras) {
        numSprings += extras + 1;
    }

    public int getSprings() {
        return numSprings;
    }

    private void addSprings(int[] extras) {
        for (int i : extras) {
            numSprings += i;
        }
    }

    @Generator
    public static BadPrimitiveArrayParameterizedMethodAccessWidget generate(int complexity, Random random) {
        BadPrimitiveArrayParameterizedMethodAccessWidget widget = new BadPrimitiveArrayParameterizedMethodAccessWidget(random.nextInt(complexity + 1));
        return widget;
    }

    @Next
    public static BadPrimitiveArrayParameterizedMethodAccessWidget next(BadPrimitiveArrayParameterizedMethodAccessWidget current, int iteration, Random random) {
        current.addSprings(new int[] {iteration, random.nextInt(iteration + 1)});
        return current;
    }

}
