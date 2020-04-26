package examples.proxy.reference;

import edu.illinois.cs.cs125.answerable.annotations.Generator;
import edu.illinois.cs.cs125.answerable.annotations.Solution;
import edu.illinois.cs.cs125.answerable.annotations.Verify;
import edu.illinois.cs.cs125.answerable.api.*;
import org.junit.jupiter.api.Assertions;

import java.util.Random;

public class InstanceofInnerClassWidget {

    @Solution
    public void doNothing(int widgets) {
        // Does nothing
    }

    public Object getInner(int widgets) {
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
    public static InstanceofInnerClassWidget generate(int complexity, Random random) {
        return new InstanceofInnerClassWidget();
    }

    @Verify
    public static void verify(TestOutput<InstanceofInnerClassWidget> ours, TestOutput<InstanceofInnerClassWidget> theirs) {
        Object theirObj = theirs.getReceiver().getInner((int) theirs.getArgs()[0]);
        Assertions.assertTrue(theirObj instanceof NamedInner);
        NamedInner theirInner = (NamedInner) theirObj;
        Assertions.assertEquals(((NamedInner) ours.getReceiver().getInner((int) ours.getArgs()[0])).getWidgets(), theirInner.getWidgets());
    }

}
