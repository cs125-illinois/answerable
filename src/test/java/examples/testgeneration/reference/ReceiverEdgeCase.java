package examples.testgeneration.reference;

import edu.illinois.cs.cs125.answerable.EdgeCase;
import edu.illinois.cs.cs125.answerable.Generator;
import edu.illinois.cs.cs125.answerable.Solution;

import java.util.Random;

public class ReceiverEdgeCase {

    private boolean edge;

    public ReceiverEdgeCase(boolean edge) {
        this.edge = edge;
    }

    @Solution
    public boolean isEdge() {
        return edge;
    }

    @Generator
    public static ReceiverEdgeCase gen(int complexity, Random random) {
        return new ReceiverEdgeCase(false);
    }

    @EdgeCase
    public static ReceiverEdgeCase[] edgeCases() {
        return new ReceiverEdgeCase[] { new ReceiverEdgeCase(true), null};
    }

}
