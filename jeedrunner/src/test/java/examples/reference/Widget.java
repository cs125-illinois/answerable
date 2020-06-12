package examples.reference;

import edu.illinois.cs.cs125.answerable.annotations.*;
import java.util.Random;

public class Widget {

  private int springs;

  public Widget(int setSprings) {
    springs = setSprings;
  }

  @Solution
  public int getSprings() {
    return springs;
  }

  @Generator
  public static Widget generate(int complexity, Random random) {
    return new Widget(complexity);
  }
}
