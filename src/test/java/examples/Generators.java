package examples;

import edu.illinois.cs.cs125.answerable.Generator;
import edu.illinois.cs.cs125.answerable.Solution;

public class Generators {
    int a = -10;

    @Solution
    int test(boolean b, Generators g) {
        if (b) {
            return 0;
        }
        return g.a + a;
    }

    @Generator
    static Generators gen(int comp) {
        Generators g = new Generators();
        g.a = comp;
        return g;
    }

    @Override
    public String toString() {
        return "Generator: " + a;
    }
}
