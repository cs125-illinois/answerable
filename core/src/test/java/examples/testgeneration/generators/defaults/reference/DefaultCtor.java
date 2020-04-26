package examples.testgeneration.generators.defaults.reference;

import edu.illinois.cs.cs125.answerable.annotations.Solution;

public class DefaultCtor {

    private int count;

    public DefaultCtor() {
        count = 100;
    }

    @Solution
    int accumulate(int n) {
        count += n;
        return count;
    }

}
