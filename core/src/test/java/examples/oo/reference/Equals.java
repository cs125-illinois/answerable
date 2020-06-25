package examples.oo.reference;

import static org.junit.jupiter.api.Assertions.assertEquals;

import edu.illinois.cs.cs125.answerable.annotations.Generator;
import edu.illinois.cs.cs125.answerable.annotations.Verify;
import edu.illinois.cs.cs125.answerable.api.TestOutput;

import java.util.Random;

public class Equals {
  private int value;
  public Equals(int setValue) {
    value = setValue;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Equals)) {
      return false;
    }
    Equals other = (Equals) o;
    return value == other.value;
  }

  @Verify(standalone = true)
  private static void verify(TestOutput<Equals> ours, TestOutput<Equals> theirs) {
    assertEquals(ours.getReceiver(), theirs.getReceiver());
  }

  @Generator
  private static Equals generate(int complexity, Random random) {
    return new Equals(complexity);
  }
}
