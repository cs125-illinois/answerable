package edu.illinois.cs.cs125.answerable.annotations.fixtures.processor;

import edu.illinois.cs.cs125.answerable.annotations.Solution;
import edu.illinois.cs.cs125.answerable.annotations.Verify;
import edu.illinois.cs.cs125.answerable.api.TestOutput;
import org.jetbrains.annotations.NotNull;

public class IsComparable implements Comparable<IsComparable> {
  private int num;

  public IsComparable(int setNum) {
    num = setNum;
  }

  @Solution
  @Override
  public int compareTo(@NotNull IsComparable o) {
    if (num == o.num) return 0;
    return num - o.num > 0 ? 1 : -1;
  }
}
