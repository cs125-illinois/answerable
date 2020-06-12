package examples.proxy.reference;

import edu.illinois.cs.cs125.answerable.annotations.Generator;
import edu.illinois.cs.cs125.answerable.annotations.Solution;
import edu.illinois.cs.cs125.answerable.annotations.Verify;
import edu.illinois.cs.cs125.answerable.api.TestOutput;
import java.util.Random;
import org.junit.jupiter.api.Assertions;

public class CollidingInnerClassWidget {

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

  private class CollidingInner {
    // Not part of the contract
  }

  @Generator
  public static CollidingInnerClassWidget generate(int complexity, Random random) {
    return new CollidingInnerClassWidget();
  }

  @Verify
  public static void verify(
      TestOutput<CollidingInnerClassWidget> ours, TestOutput<CollidingInnerClassWidget> theirs) {
    Object theirObj = theirs.getReceiver().getInner((int) theirs.getArgs()[0]);
    Assertions.assertTrue(theirObj instanceof NamedInner);
    NamedInner theirInner = (NamedInner) theirObj;
    Assertions.assertEquals(
        ((NamedInner) ours.getReceiver().getInner((int) ours.getArgs()[0])).getWidgets(),
        theirInner.getWidgets());
  }
}
