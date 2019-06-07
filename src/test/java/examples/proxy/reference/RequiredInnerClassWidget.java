package examples.proxy.reference;

import edu.illinois.cs.cs125.answerable.Generator;
import edu.illinois.cs.cs125.answerable.Solution;
import edu.illinois.cs.cs125.answerable.TestOutput;
import edu.illinois.cs.cs125.answerable.Verify;
import org.junit.jupiter.api.Assertions;

import java.util.Random;

public class RequiredInnerClassWidget {

    @Solution
    public void doNothing(int widgets) {
        // Does nothing
    }

    public NamedInner getInner(int widgets) {
        NamedInner inner = new NamedInner();
        inner.widgets = widgets;
        return inner;
    }

    public class NamedInner {
        private int widgets;
        public int getWidgets() {
            return widgets;
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
