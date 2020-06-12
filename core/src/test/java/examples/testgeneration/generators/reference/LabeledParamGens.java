package examples.testgeneration.generators.reference;

import static org.junit.jupiter.api.Assertions.assertTrue;

import edu.illinois.cs.cs125.answerable.annotations.*;
import edu.illinois.cs.cs125.answerable.api.*;
import java.util.Random;

public class LabeledParamGens {
  @Solution(enabled = "default")
  static int testMethod(int usesDefault, @UseGenerator(name = "override") int isOverridden) {
    return isOverridden;
  }

  @Generator(name = "override")
  static int gen(int complexity, Random r) {
    return 0;
  }

  @Generator(name = "default")
  static int gen2(int c, Random r) {
    return Generators.defaultIntGenerator(c, r);
  }

  @EdgeCase static int[] intEdgeCases = {};
  @SimpleCase static int[] intSimpleCases = {};

  @Verify
  static void verify(TestOutput ours, TestOutput theirs) {
    assertTrue((Integer) ours.getOutput() == 0 && (Integer) theirs.getOutput() == 0);
  }
}
