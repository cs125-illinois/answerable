package edu.illinois.cs.cs125.answerable.annotations.fixtures.generator;

import edu.illinois.cs.cs125.answerable.annotations.Generator;
import java.util.Random;

public class Broken0 {
  @Generator
  public int broken0(int complexity, Random random) {
    return 0;
  }

  @Generator
  public static void broken1(int complexity) {
    return;
  }
}
