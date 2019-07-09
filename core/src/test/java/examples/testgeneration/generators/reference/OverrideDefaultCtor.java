package examples.testgeneration.generators.reference;

import edu.illinois.cs.cs125.answerable.api.Generator;
import edu.illinois.cs.cs125.answerable.api.Solution;

import java.util.Random;

public class OverrideDefaultCtor {

    private int springs;

    @Solution
    public int getSprings() {
        return springs;
    }

    public void setSprings(int count) {
        springs = count;
    }

    @Generator
    public static OverrideDefaultCtor generate(int complexity, Random random) {
        OverrideDefaultCtor obj = new OverrideDefaultCtor();
        obj.setSprings(22);
        return obj;
    }

}
