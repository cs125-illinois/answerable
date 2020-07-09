package edu.illinois.cs.cs125.answerable.annotations.fixtures.processor;

import edu.illinois.cs.cs125.answerable.annotations.Solution;

public class StaticFactory {
  private int num;

  public StaticFactory(int setNum) {
    num = setNum;
  }

  @Solution
  int getNum() {
    return num;
  }

  public static StaticFactory factory(int setNum) {
    return new StaticFactory(setNum);
  }
}
