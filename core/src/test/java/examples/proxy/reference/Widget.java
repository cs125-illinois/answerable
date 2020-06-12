package examples.proxy.reference;

import edu.illinois.cs.cs125.answerable.annotations.Generator;
import edu.illinois.cs.cs125.answerable.annotations.Solution;
import edu.illinois.cs.cs125.answerable.annotations.Verify;
import edu.illinois.cs.cs125.answerable.api.*;
import java.util.Random;
import org.junit.jupiter.api.Assertions;

public class Widget {

  private int springs;

  @Solution
  public void positionSprings(int numSprings) {
    springs = Math.abs(numSprings);
  }

  public String[] getSpringPositions() {
    String[] positions = new String[springs];
    for (int i = 0; i < springs; i++) {
      positions[i] = "Spring " + (i + 1);
    }
    return positions;
  }

  @Verify
  public static void verify(TestOutput<Widget> ours, TestOutput<Widget> theirs) {
    Assertions.assertArrayEquals(
        ours.getReceiver().getSpringPositions(), theirs.getReceiver().getSpringPositions());
  }

  @Generator
  public static Widget generator(int complexity, Random random) {
    return new Widget();
  }
}
