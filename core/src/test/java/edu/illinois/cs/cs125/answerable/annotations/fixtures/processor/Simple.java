package edu.illinois.cs.cs125.answerable.annotations.fixtures.processor;

import edu.illinois.cs.cs125.answerable.annotations.Solution;

public class Simple {
  @Solution
  public boolean intDouble(int a, double b) {
    return Math.abs(b - (double) a) < 0.5;
  }
}
