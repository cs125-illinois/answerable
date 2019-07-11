package examples.testgeneration.argsannotated.reference;

import edu.illinois.cs.cs125.answerable.api.DefaultTestRunArguments;
import edu.illinois.cs.cs125.answerable.api.TestOutput;
import edu.illinois.cs.cs125.answerable.api.Verify;

public class ArgsOnStandaloneVerify {

    @Verify(standalone = true)
    @DefaultTestRunArguments(numTests = 96)
    public static void verify(TestOutput<ArgsOnStandaloneVerify> ours, TestOutput<ArgsOnStandaloneVerify> theirs) {
        // Do nothing
    }

}
