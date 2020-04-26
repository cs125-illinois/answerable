package edu.illinois.cs.cs125.answerable.annotations.fixtures;

import edu.illinois.cs.cs125.answerable.annotations.DefaultTestRunArguments;
import edu.illinois.cs.cs125.answerable.annotations.Solution;
import edu.illinois.cs.cs125.answerable.annotations.Verify;

public class TestValidateDefaultTestRunArguments {
    @DefaultTestRunArguments
    @Solution
    public void correct1() {}

    @DefaultTestRunArguments
    @Verify(standalone = true)
    public void correct2() {}

    @DefaultTestRunArguments
    public void broken1() {}

    @DefaultTestRunArguments
    @Verify
    public void broken2() {}
}
