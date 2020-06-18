package edu.illinois.cs.cs125.answerable.annotations.fixtures;

import edu.illinois.cs.cs125.answerable.annotations.Next;
import java.util.Random;

public class TestValidateNext {
  // this is the only correct format
  @Next
  public static TestValidateNext correct0(TestValidateNext o, int iteration, Random random) {
    return null;
  }

  // should be static
  @Next
  public TestValidateNext broken0(TestValidateNext o, int iteration, Random random) {
    return null;
  }

  // missing a bunch of arguments
  @Next
  public static TestValidateNext broken1(int iteration) {
    return null;
  }

  // missing `current` argument
  @Next
  public static TestValidateNext broken2(int iteration, Random random) {
    return null;
  }

  // extra argument on the end
  @Next
  public static TestValidateNext broken3(
      TestValidateNext o, int iteration, Random random, boolean b) {
    return o;
  }

  // extra arguments at the beginning
  @Next
  public static TestValidateNext broken4(
      boolean b, TestValidateNext o, int iteration, Random random) {
    return o;
  }

  // return type is wrong
  @Next
  public static int broken5(TestValidateNext o, int iteration, Random random) {
    return 0;
  }

  // `current` type is wrong
  @Next
  public static TestValidateNext broken6(int i, int iteration, Random random) {
    return null;
  }
}
