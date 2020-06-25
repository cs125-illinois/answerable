package edu.illinois.cs.cs125.answerable.testing.fixtures.oo.reference;

import edu.illinois.cs.cs125.answerable.annotations.Generator;
import edu.illinois.cs.cs125.answerable.annotations.Solution;
import java.util.Random;

// TODO: Enable @Solution on class level to test multiple methods
@Solution
public class GetSetEquals {
  private int value;

  public GetSetEquals(int setValue) {
    value = setValue;
  }

  public int getValue() {
    return value;
  }

  public void setValue(int setValue) {
    value = setValue;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof GetSetEquals)) {
      return false;
    }
    GetSetEquals other = (GetSetEquals) o;
    return value == other.value;
  }

  @Generator
  private static Object generate(int complexity, Random random) {
    switch (random.nextInt() % 3) {
      case 0:
        return new GetSetEquals(complexity);
      case 1:
        return new Object();
      case 2:
      default:
        return "String";
    }
  }
}
