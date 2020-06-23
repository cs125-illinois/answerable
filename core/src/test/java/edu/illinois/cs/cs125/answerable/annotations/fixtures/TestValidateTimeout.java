package edu.illinois.cs.cs125.answerable.annotations.fixtures;

import edu.illinois.cs.cs125.answerable.annotations.Solution;
import edu.illinois.cs.cs125.answerable.annotations.Timeout;
import edu.illinois.cs.cs125.answerable.annotations.Verify;
import edu.illinois.cs.cs125.answerable.api.TestOutput;

public class TestValidateTimeout {
  @Timeout(timeout = 1)
  @Solution
  public void correct0() {}

  @Timeout(timeout = 1)
  @Verify(standalone = true)
  public void correct1(TestOutput ours, TestOutput theirs) {}

  @Timeout
  @Solution
  public void broken0() {}

  @Timeout(timeout = 0)
  @Solution
  public void broken1() {}

  @Timeout(timeout = -1)
  @Solution
  public void broken2() {}

  @Timeout(timeout = 1)
  @Verify
  public void broken3(TestOutput ours, TestOutput theirs) {}

  @Timeout(timeout = 1)
  public void broken4() {}
}
