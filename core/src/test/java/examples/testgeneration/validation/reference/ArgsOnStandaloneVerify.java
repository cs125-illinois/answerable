package examples.testgeneration.validation.reference;

import edu.illinois.cs.cs125.answerable.annotations.DefaultTestRunArguments;
import edu.illinois.cs.cs125.answerable.annotations.Verify;
import edu.illinois.cs.cs125.answerable.api.TestOutput;

public class ArgsOnStandaloneVerify {

  @Verify(standalone = true)
  @DefaultTestRunArguments(numTests = 96)
  public static void verify(
      TestOutput<ArgsOnStandaloneVerify> ours, TestOutput<ArgsOnStandaloneVerify> theirs) {
    // Do nothing
  }
}
