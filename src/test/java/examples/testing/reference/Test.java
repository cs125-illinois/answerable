package examples.testing.reference;

import edu.illinois.cs.cs125.answerable.*;

import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Test {

    private static int test = 0;

    @Solution(prints = true)
    public static boolean test(String[] is) {
        System.out.println(test++);
        return true;
    }

    @Verify
    public static void verify(TestOutput<Test> ours, TestOutput<Test> theirs) {
        System.out.println("Ours: " + ours.getStdOut() + " Theirs: " + theirs.getStdOut());
        assertEquals(ours.getStdOut(), theirs.getStdOut());
    }

    @Generator
    public static Test gen(int complexity, Random r) {
        return new Test();
    }

    @Generator
    public static List<Integer> genInt(int complexity, Random r) {
        return List.of(r.nextInt());
    }

    //@Generator
    public static List<Character> genInt2(int complexity, Random r) {
        return List.of('a');
    }
}
