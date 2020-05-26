package edu.illinois.cs.cs125.answerable.annotations.fixtures;

import edu.illinois.cs.cs125.answerable.annotations.Timeout;

public class TestValidateTimeout {
    @Timeout(timeout = 1)
    public void correct0() { }

    @Timeout
    public void broken0() { }

    @Timeout(timeout = 0)
    public void broken1() { }

    @Timeout(timeout = -1)
    public void broken2() { }
}
