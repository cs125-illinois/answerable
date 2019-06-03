package examples.testing.reference;

import edu.illinois.cs.cs125.answerable.Generator;
import edu.illinois.cs.cs125.answerable.Solution;

import java.util.Random;

public class Test {
    @Solution
    public static boolean test(Test t) {
        return true;
    }

    @Generator
    public static Test gen(int complexity, Random r) {
        return new Test();
    }
}
