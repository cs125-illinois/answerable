package examples.proxy.reference;

import edu.illinois.cs.cs125.answerable.annotations.Generator;
import edu.illinois.cs.cs125.answerable.annotations.Solution;
import edu.illinois.cs.cs125.answerable.annotations.Verify;
import edu.illinois.cs.cs125.answerable.api.Generators;
import edu.illinois.cs.cs125.answerable.api.TestOutput;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WidgetArgumentWidget {

    private String name;
    private int springs;

    public WidgetArgumentWidget(String setName, int setSprings) {
        name = setName;
        springs = setSprings;
    }

    public int getSprings() {
        return springs;
    }

    public String getName() {
        return name;
    }

    @Solution
    public void copyNameFrom(WidgetArgumentWidget other) {
        name = other.name;
    }

    @Generator
    public static WidgetArgumentWidget generate(int complexity, Random random) {
        return new WidgetArgumentWidget(
                Generators.defaultStringGenerator(complexity, random),
                Math.max(Generators.defaultIntGenerator(complexity, random), 0)
        );
    }

    @Verify
    public static void verify(TestOutput<WidgetArgumentWidget> ours, TestOutput<WidgetArgumentWidget> theirs) {
        assertEquals(theirs.getReceiver().getName(), ((WidgetArgumentWidget) theirs.getArgs()[0]).getName());
    }

}
