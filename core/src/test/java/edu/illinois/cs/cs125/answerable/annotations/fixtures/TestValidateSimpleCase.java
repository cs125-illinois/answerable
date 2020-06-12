package edu.illinois.cs.cs125.answerable.annotations.fixtures;

import edu.illinois.cs.cs125.answerable.annotations.EdgeCase;
import edu.illinois.cs.cs125.answerable.annotations.SimpleCase;

public class TestValidateSimpleCase {
  @SimpleCase public static int[] correct0 = new int[] {1, 2, 5};

  @SimpleCase
  public static int[] correct1() {
    return new int[] {};
  }

  @SimpleCase public static String[] correct2 = new String[] {"1", "2", "5"};

  @SimpleCase
  public static String[] correct3() {
    return new String[] {};
  }

  @EdgeCase @SimpleCase public static int[] broken0 = new int[] {1, 2, 5};
  @SimpleCase public int[] broken1 = new int[] {1, 2, 5};

  @SimpleCase
  public int[] broken2() {
    return new int[] {};
  }

  @SimpleCase public int broken3 = 125;

  @SimpleCase
  public int broken4() {
    return 125;
  }

  @EdgeCase @SimpleCase public static String[] broken5 = new String[] {"1", "2", "5"};
  @SimpleCase public String[] broken6 = new String[] {"1", "2", "5"};

  @SimpleCase
  public String[] broken7() {
    return new String[] {};
  }

  @SimpleCase public String broken8 = "125";

  @SimpleCase
  public String broken9() {
    return "125";
  }
}
