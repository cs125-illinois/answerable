package examples.sorting.reference;

import edu.illinois.cs.cs125.answerable.annotations.Solution;
import edu.illinois.cs.cs125.answerable.annotations.Verify;
import edu.illinois.cs.cs125.answerable.api.TestOutput;
import java.util.Arrays;
import org.junit.jupiter.api.Assertions;

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
