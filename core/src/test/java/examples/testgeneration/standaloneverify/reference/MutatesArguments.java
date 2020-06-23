package examples.testgeneration.standaloneverify.reference;

import edu.illinois.cs.cs125.answerable.annotations.Solution;
import edu.illinois.cs.cs125.answerable.annotations.Verify;
import edu.illinois.cs.cs125.answerable.api.TestOutput;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class MutatesArguments {
  @Solution
  public static int[] soln(int[] ints) {
    return ints;
  }

  @Verify
  private static void verify(TestOutput ours, TestOutput theirs) {
    int[] ourArgs = (int[]) ours.getArgs()[0];
    int[] theirArgs = (int[]) theirs.getArgs()[0];

    assertArrayEquals(ourArgs, theirArgs, "Arg array was changed");
  }
}
