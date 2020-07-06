package edu.illinois.cs.cs125.answerable.testing.fixtures.GeneratorMap;

import edu.illinois.cs.cs125.answerable.testing.library.Location;

public class MultipleFunctions {
  public static boolean isZero(int a) {
    return a == 0;
  }

  public static boolean usesArray(boolean[] arr) {
    if (arr.length == 0) return true;
    return arr[0];
  }

  public static double manyArgs(double a, Location loc, String str) {
    return loc.getLatitude();
  }
}
