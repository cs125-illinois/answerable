package examples.testgeneration.mutatestaticfield.reference;

import edu.illinois.cs.cs125.answerable.annotations.Solution;
import edu.illinois.cs.cs125.answerable.annotations.Timeout;

public class Counter {

  private static int count;

  @Solution
  @Timeout(timeout = 1000)
  public static int increment() {
    count++;
    return count;
  }
}
