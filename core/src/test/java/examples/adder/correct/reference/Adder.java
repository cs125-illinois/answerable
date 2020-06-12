package examples.adder.correct.reference;

import edu.illinois.cs.cs125.answerable.annotations.EdgeCase;
import edu.illinois.cs.cs125.answerable.annotations.Solution;

public final class Adder {
  /*
   * Probably the easiest possible example.
   */
  @Solution
  public static int add(int first, int second) {
    return first + second;
  }

  @EdgeCase public static int[] intEdgeCases = new int[] {-1, 0, 1};
}
