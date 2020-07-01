package edu.illinois.cs.cs125.answerable.testing.fixtures.GeneratorMap;

public class CountsInstances {
  private static int instances = 0;
  public CountsInstances() {
    instances++;
  }

  public static int getInstances() {
    return instances;
  }

  public static int getGeneratorCalls() {
    return 0; // no generator
  }
}
