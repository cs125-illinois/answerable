package edu.illinois.cs.cs125.answerable.testing.fixtures.GeneratorMap;

import edu.illinois.cs.cs125.answerable.annotations.Generator;

import java.util.Random;

public class CountsGenerators {
  private static int generatorCalls = 0;
  private static int nonGeneratedInstances = 0;

  public CountsGenerators() {
    nonGeneratedInstances++;
  }

  @Generator
  private static CountsGenerators generate(int c, Random r) {
    generatorCalls++;
    nonGeneratedInstances--; // will get incremented back by constructor
    return new CountsGenerators();
  }

  public static int getGeneratorCalls() {
    return generatorCalls;
  }

  public static int getNonGeneratedInstances() {
    return nonGeneratedInstances;
  }
}
