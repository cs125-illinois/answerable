package edu.illinois.cs.cs125.answerable.annotations.fixtures.generator;

import edu.illinois.cs.cs125.answerable.annotations.Generator;
import java.util.Random;

public class Correct0 {
  @Generator
  public static int correct0(int complexity, Random random) {
    return 0;
  }

  @Generator
  public static void correct1(int complexity, Random random) {
    return;
  }
}
