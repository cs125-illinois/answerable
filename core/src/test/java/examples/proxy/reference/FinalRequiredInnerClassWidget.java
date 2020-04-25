package examples.proxy.reference;

import edu.illinois.cs.cs125.answerable.Generator;
import edu.illinois.cs.cs125.answerable.Solution;
import edu.illinois.cs.cs125.answerable.Verify;
import edu.illinois.cs.cs125.answerable.api.*;
import org.junit.jupiter.api.Assertions;

import java.util.Random;

public final class FinalRequiredInnerClassWidget {

    @Solution
    public final void doNothing(int widgets) {
        // Does nothing
    }

    public final NamedInner getInner(int widgets) {
        NamedInner inner = new NamedInner();
        inner.soln_widgets = widgets;
        return inner;
    }

    public final class NamedInner {
        private int soln_widgets;
        public final int getWidgets() {
            return soln_widgets;
        }
    }

    @Generator
    public static FinalRequiredInnerClassWidget generate(int complexity, Random random) {
        return new FinalRequiredInnerClassWidget();
    }

    @Verify
    public static void verify(TestOutput<FinalRequiredInnerClassWidget> ours, TestOutput<FinalRequiredInnerClassWidget> theirs) {
        Assertions.assertEquals(ours.getReceiver().getInner((int) ours.getArgs()[0]).getWidgets(),
                theirs.getReceiver().getInner((int) ours.getArgs()[0]).getWidgets());
    }

}
