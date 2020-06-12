package edu.illinois.cs.cs125.answerable.annotations.fixtures;

import edu.illinois.cs.cs125.answerable.annotations.EdgeCase;
import edu.illinois.cs.cs125.answerable.annotations.SimpleCase;

public class TestValidateEdgeCase {
  @EdgeCase public static int[] correct0 = new int[] {1, 2, 5};

  @EdgeCase
  public static int[] correct1() {
    return new int[] {};
  }

  @EdgeCase public static String[] correct2 = new String[] {"1", "2", "5"};

  @EdgeCase
  public static String[] correct3() {
    return new String[] {};
  }

  @SimpleCase @EdgeCase public static int[] broken0 = new int[] {1, 2, 5};
  @EdgeCase public int[] broken1 = new int[] {1, 2, 5};

  @EdgeCase
  public int[] broken2() {
    return new int[] {};
  }

  @EdgeCase public int broken3 = 125;

  @EdgeCase
  public int broken4() {
    return 125;
  }

  @SimpleCase @EdgeCase public static String[] broken5 = new String[] {"1", "2", "5"};
  @EdgeCase public String[] broken6 = new String[] {"1", "2", "5"};

  @EdgeCase
  public String[] broken7() {
    return new String[] {};
  }

  @EdgeCase public String broken8 = "125";

  @EdgeCase
  public String broken9() {
    return "125";
  }
}
