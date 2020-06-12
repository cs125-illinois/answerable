package examples.testing.reference;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.illinois.cs.cs125.answerable.*;
import edu.illinois.cs.cs125.answerable.annotations.Generator;
import edu.illinois.cs.cs125.answerable.annotations.Solution;
import edu.illinois.cs.cs125.answerable.annotations.Timeout;
import edu.illinois.cs.cs125.answerable.annotations.Verify;
import edu.illinois.cs.cs125.answerable.api.*;
import java.util.List;
import java.util.Random;

public class Test {

  private static int test = 0;

  @Solution
  @Timeout(timeout = 1000)
  public static int test(List<Integer> ss) {
    return test;
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
}
