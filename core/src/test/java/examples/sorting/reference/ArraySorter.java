package examples.sorting.reference;

import edu.illinois.cs.cs125.answerable.api.Solution;
import edu.illinois.cs.cs125.answerable.api.TestOutput;
import edu.illinois.cs.cs125.answerable.api.Verify;
import org.junit.jupiter.api.Assertions;

import java.util.Arrays;

public class ArraySorter {

    @Solution
    public static int[] sort(int[] toSort) {
        Arrays.sort(toSort);
        return toSort;
    }

    @Verify
    public static void verify(TestOutput<Void> ours, TestOutput<Void> theirs) {
        Assertions.assertArrayEquals((int[]) ours.getOutput(), (int[]) theirs.getOutput());
    }

}
