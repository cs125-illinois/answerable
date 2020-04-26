package examples.proxy.reference;

import edu.illinois.cs.cs125.answerable.annotations.Generator;
import edu.illinois.cs.cs125.answerable.annotations.Solution;
import edu.illinois.cs.cs125.answerable.annotations.Verify;
import edu.illinois.cs.cs125.answerable.api.*;
import org.junit.jupiter.api.Assertions;

import java.util.Random;

public class RequiredInnerClassWidget {

    @Solution
    public void doNothing(int widgets) {
        // Does nothing
    }

    public NamedInner getInner(int widgets) {
        NamedInner inner = new NamedInner();
        inner.soln_widgets = widgets;
        return inner;
    }

    public class NamedInner {
        private int soln_widgets;
        public int getWidgets() {
            return soln_widgets;
        }
    }

    @Generator
    public static RequiredInnerClassWidget generate(int complexity, Random random) {
        return new RequiredInnerClassWidget();
    }

    @Verify
    public static void verify(TestOutput<RequiredInnerClassWidget> ours, TestOutput<RequiredInnerClassWidget> theirs) {
        Assertions.assertEquals(ours.getReceiver().getInner((int) ours.getArgs()[0]).getWidgets(),
                theirs.getReceiver().getInner((int) ours.getArgs()[0]).getWidgets());
    }

}
