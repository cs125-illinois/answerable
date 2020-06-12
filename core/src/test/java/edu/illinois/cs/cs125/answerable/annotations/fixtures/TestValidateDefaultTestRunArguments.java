package edu.illinois.cs.cs125.answerable.annotations.fixtures;

import edu.illinois.cs.cs125.answerable.annotations.DefaultTestRunArguments;
import edu.illinois.cs.cs125.answerable.annotations.Solution;
import edu.illinois.cs.cs125.answerable.annotations.Verify;

public class TestValidateDefaultTestRunArguments {
  @DefaultTestRunArguments
  @Solution
  public void correct0() {}

  @DefaultTestRunArguments
  @Verify(standalone = true)
  public void correct1() {}

  @DefaultTestRunArguments
  public void broken0() {}

  @DefaultTestRunArguments
  @Verify
  public void broken1() {}
}
