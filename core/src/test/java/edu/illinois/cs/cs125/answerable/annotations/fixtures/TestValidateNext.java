package edu.illinois.cs.cs125.answerable.annotations.fixtures;

import edu.illinois.cs.cs125.answerable.annotations.Next;
import java.util.Random;

public class TestValidateNext {
  @Next
  public static int correct0(Object o, int complexity, Random random) {
    return 0;
  }

  @Next
  public static void correct1(Object b, int complexity, Random random) {
    return;
  }

  @Next
  public int broken0(int complexity, Random random) {
    return 0;
  }

  @Next
  public static void broken1(int complexity) {
    return;
  }
}
