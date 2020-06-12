package examples.proxy.reference;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.illinois.cs.cs125.answerable.annotations.Generator;
import edu.illinois.cs.cs125.answerable.annotations.Verify;
import edu.illinois.cs.cs125.answerable.api.Generators;
import edu.illinois.cs.cs125.answerable.api.TestOutput;
import java.util.Random;

public class CopyableWidget {

  private int springs;

  public CopyableWidget(int setSprings) {
    springs = setSprings;
  }

  public CopyableWidget copy() {
    return new CopyableWidget(springs);
  }

  public String checkEquivalence(CopyableWidget other) {
    if (other == null) return "null";
    if (other == this) return "reference-equal";
    if (other.springs == springs) return "value-equal";
    return "different";
  }

  @Generator
  public static CopyableWidget generate(int complexity, Random random) {
    return new CopyableWidget(Generators.defaultIntGenerator(complexity, random));
  }

  @Verify(standalone = true)
  public static void verify(TestOutput<CopyableWidget> ours, TestOutput<CopyableWidget> theirs) {
    assertEquals(
        ours.getReceiver().checkEquivalence(null), theirs.getReceiver().checkEquivalence(null));
    assertEquals("reference-equal", theirs.getReceiver().checkEquivalence(theirs.getReceiver()));
    assertEquals(
        ours.getReceiver().checkEquivalence(ours.getReceiver().copy()),
        theirs.getReceiver().checkEquivalence(theirs.getReceiver().copy()));
  }
}
