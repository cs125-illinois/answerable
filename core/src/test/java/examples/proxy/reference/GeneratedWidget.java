package examples.proxy.reference;

import edu.illinois.cs.cs125.answerable.annotations.Generator;
import edu.illinois.cs.cs125.answerable.annotations.Solution;
import edu.illinois.cs.cs125.answerable.annotations.Verify;
import edu.illinois.cs.cs125.answerable.api.*;
import java.util.Random;
import org.junit.jupiter.api.Assertions;

public class GeneratedWidget {

  private int numSprings;

  public GeneratedWidget(int springs) {
    numSprings = springs;
  }

  @Solution
  public void moreSprings(int extras) {
    numSprings += extras + 1;
  }

  public int getSprings() {
    return numSprings;
  }

  @Generator
  public static GeneratedWidget generate(int complexity, Random random) {
    // Use an array to test bytecode transformation of MULTIANEWARRAY
    GeneratedWidget[][] widgets = new GeneratedWidget[1][1];
    widgets[0][0] = new GeneratedWidget(random.nextInt(complexity + 1));
    return widgets[0][0];
  }

  @Verify
  public static void verify(TestOutput<GeneratedWidget> ours, TestOutput<GeneratedWidget> theirs) {
    Assertions.assertEquals(ours.getReceiver().getSprings(), theirs.getReceiver().getSprings());
  }
}
