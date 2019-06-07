package examples.verify;

import edu.illinois.cs.cs125.answerable.*;
import examples.proxy.reference.GeneratedWidget;
import org.junit.jupiter.api.Assertions;

import java.util.Random;

public class BadPrimitiveParameterizedMethodAccessWidget {

    private int numSprings;

    public BadPrimitiveParameterizedMethodAccessWidget(int springs) {
        numSprings = springs;
    }

    @Solution
    public void moreSprings(int extras) {
        numSprings += extras + 1;
    }

    public int getSprings() {
        return numSprings;
    }

    private void addSprings(int extras) {
        numSprings += extras;
    }

    @Generator
    public static BadPrimitiveParameterizedMethodAccessWidget generate(int complexity, Random random) {
        BadPrimitiveParameterizedMethodAccessWidget widget = new BadPrimitiveParameterizedMethodAccessWidget(random.nextInt(complexity + 1));
        return widget;
    }

    @Next
    public static BadPrimitiveParameterizedMethodAccessWidget next(BadPrimitiveParameterizedMethodAccessWidget current, int iteration, Random random) {
        current.addSprings(iteration);
        return current;
    }

}
