package edu.illinois.cs.cs125.answerable.annotations.fixtures;

import edu.illinois.cs.cs125.answerable.annotations.EdgeCase;
import edu.illinois.cs.cs125.answerable.annotations.SimpleCase;

public class TestValidateCaseMethod {
  @SimpleCase
  public static int[] correct0() {
    return new int[] {};
  }

  @EdgeCase
  public static int[] correct1() {
    return new int[] {};
  }

  @SimpleCase
  public static String[] correct2() {
    return new String[] {};
  }

  @EdgeCase
  public static String[] correct3() {
    return new String[] {};
  }

  @SimpleCase
  @EdgeCase
  public static String[] broken0() {
    return new String[] {};
  }

  @SimpleCase
  public int[] broken1() {
    return new int[] {};
  }

  @SimpleCase
  public static int[] broken2(int unused) {
    return new int[] {};
  }

  @SimpleCase
  public static int broken3() {
    return 0;
  }

  @EdgeCase
  public String[] broken4() {
    return new String[] {};
  }

  @EdgeCase
  public static String[] broken5(int unused) {
    return new String[] {};
  }

  @EdgeCase
  public static String broken6() {
    return "";
  }
}
