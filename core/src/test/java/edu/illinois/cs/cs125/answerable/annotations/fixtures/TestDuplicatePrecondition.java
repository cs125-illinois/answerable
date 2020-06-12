package edu.illinois.cs.cs125.answerable.annotations.fixtures;

import edu.illinois.cs.cs125.answerable.annotations.Precondition;
import edu.illinois.cs.cs125.answerable.annotations.Solution;

public class TestDuplicatePrecondition {
  @Solution
  public void solution(int unused) {}

  @Precondition
  public boolean broken_0(boolean unused) {
    return true;
  }

  @Precondition
  public boolean broken_1(boolean unused) {
    return true;
  }
}
