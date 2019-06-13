package examples.testing.reference;

import edu.illinois.cs.cs125.answerable.*;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Test {

    private static int test = 0;

    @Solution
    public static int test(List<Integer> ss) {
        return test++;
    }

    @Verify
    public static void verify(TestOutput<Test> ours, TestOutput<Test> theirs) {
        assertEquals(ours.getTypeOfBehavior(), Behavior.RETURNED);
        assertEquals(ours.getOutput(), theirs.getOutput());
    }

    @Generator
    public static Test gen(int complexity, Random r) {
        return new Test();
    }

    @Generator
    public static List<Integer> genInt(int complexity, Random r) {
        return List.of(r.nextInt());
    }

    @Generator
    public static List<Character> genInt2(int complexity, Random r) {
        return List.of('a');
    }

}
