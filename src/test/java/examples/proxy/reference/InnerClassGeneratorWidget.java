package examples.proxy.reference;

import edu.illinois.cs.cs125.answerable.*;
import org.junit.jupiter.api.Assertions;

import java.util.Random;
import java.util.stream.IntStream;

public class InnerClassGeneratorWidget {

    private int numSprings;

    @Helper
    private static int runnableRuns = 0;

    @Helper
    private static Runnable[] lastRunnerHolder;

    @Helper
    private static NamedInner lastNamedInner;

    @Helper
    static int innerRuns = 0;

    public InnerClassGeneratorWidget(int springs) {
        numSprings = springs;
    }

    @Solution
    public void moreSprings(int extras) {
        numSprings += extras + 1;
    }

    public int getSprings() {
        return numSprings;
    }

    @Generator
    public static InnerClassGeneratorWidget generate(int complexity, Random random) {
        // Intentionally not using lambda syntax to guarantee that an inner class is generated
        final InnerClassGeneratorWidget[] widgetHolder = new InnerClassGeneratorWidget[1];
        lastRunnerHolder = new Runnable[1];
        lastRunnerHolder[0] = new Runnable() {
            @Override
            public void run() {
                runnableRuns++;
                lastNamedInner = new NamedInner(new InnerClassGeneratorWidget(random.nextInt(complexity + 1)));
                widgetHolder[0] = lastNamedInner.fiddle();
                ((Runnable) () -> lastNamedInner.new InnerInner(2).doAgain()).run();
                System.out.print(", runnable ran (#" + runnableRuns + ")");
            }
        };
        lastRunnerHolder[0].run();
        System.out.println(", outer method ran (#" + runnableRuns + ")");
        return widgetHolder[0];
    }

    @Verify
    public static void verify(TestOutput<InnerClassGeneratorWidget> ours, TestOutput<InnerClassGeneratorWidget> theirs) {
        Assertions.assertEquals(ours.getReceiver().getSprings(), theirs.getReceiver().getSprings());
    }

    private static class NamedInner {
        private InnerClassGeneratorWidget widget;
        private NamedInner(InnerClassGeneratorWidget setWidget) {
            // Anonymous inner class of named inner class
            new Runnable() {
                @Override
                public void run() {
                    widget = setWidget;
                    innerRuns++;
                    System.out.print("NamedInner Runnable ran (#" + innerRuns + ")");
                }
            }.run();
        }
        private InnerClassGeneratorWidget fiddle() {
            IntStream.range(1, 2).forEach(i -> widget.moreSprings(i));
            return widget;
        }
        private class InnerInner {
            private int extras;
            private InnerInner(int setExtras) {
                extras = setExtras;
            }
            private void doAgain() {
                widget.moreSprings(extras);
            }
        }
    }
}
