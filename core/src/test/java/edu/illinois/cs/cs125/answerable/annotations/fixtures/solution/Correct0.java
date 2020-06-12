package edu.illinois.cs.cs125.answerable.annotations.fixtures.solution;

import edu.illinois.cs.cs125.answerable.annotations.Solution;

public class Correct0 {
  @Solution
  public void test() {}

  @Solution(name = "test_int")
  public void test(int unused) {}
}
