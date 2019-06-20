package examples.testgeneration.standaloneverify.reference;

import edu.illinois.cs.cs125.answerable.api.*;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Standalone {

    private int a;
    private int b;

    public Standalone(int x, int y) {
        a = x;
        b = y;
    }

    public int getA() {
        return a;
    }

    public int getB() {
        return b;
    }

    @Verify(name = "standalone test", standalone = true)
    public static void verify(TestOutput<Standalone> ours, TestOutput<Standalone> theirs) {
        Standalone ref = ours.getReceiver();
        Standalone sub = theirs.getReceiver();

        assertEquals(ref.getA(), sub.getA());
        assertEquals(ref.getB(), sub.getB());
    }

    @Generator
    public static Standalone make(int complexity, Random random) {
        int first = Generators.defaultIntGenerator(complexity, random);
        int second = Generators.defaultIntGenerator(complexity, random);

        return new Standalone(first, second);
    }

}
