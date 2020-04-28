package examples.proxy;

import edu.illinois.cs.cs125.answerable.annotations.Generator;
import edu.illinois.cs.cs125.answerable.annotations.Verify;
import edu.illinois.cs.cs125.answerable.api.Generators;
import edu.illinois.cs.cs125.answerable.api.TestOutput;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CopyableWidget {

    private int springs;

    public CopyableWidget(int setSprings) {
        springs = setSprings;
    }

    public CopyableWidget copy() {
        CopyableWidget dup = new CopyableWidget(0);
        dup.springs = springs;
        return dup;
    }

    public String checkEquivalence(CopyableWidget other) {
        if (other == null) return "null";
        if (other == this) return "reference-equal";
        if (other.springs == springs) return "value-equal";
        return "different";
    }

}
