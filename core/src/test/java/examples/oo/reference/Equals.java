package examples.oo.reference;

import edu.illinois.cs.cs125.answerable.annotations.Generator;
import edu.illinois.cs.cs125.answerable.annotations.Solution;
import edu.illinois.cs.cs125.answerable.api.Generators;
import java.util.Random;

public class Equals {
  private int value;

  public Equals(int setValue) {
    value = setValue;
  }

  @Override
  @Solution
  public boolean equals(Object o) {
    if (!(o instanceof Equals)) {
      return false;
    }
    Equals other = (Equals) o;
    return value == other.value;
  }

  /*  @Verify(standalone = true)
  private static void verify(TestOutput<Equals> ours, TestOutput<Equals> theirs) {
    return;
  }*/

  @Generator
  private static Equals generate(int complexity, Random random) {
    return new Equals(complexity);
  }

  @Generator
  private static Object genObject(int complexity, Random random) {
    if (random.nextDouble() < 0.2) {
      Generators.defaultAsciiGenerator(complexity, random);
    }
    return new Equals(complexity);
  }
}
