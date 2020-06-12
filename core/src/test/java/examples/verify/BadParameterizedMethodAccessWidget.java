package examples.verify;

import edu.illinois.cs.cs125.answerable.annotations.Generator;
import edu.illinois.cs.cs125.answerable.annotations.Solution;
import java.util.Random;

public class BadParameterizedMethodAccessWidget {

  private int numSprings;

  public BadParameterizedMethodAccessWidget(int springs) {
    numSprings = springs;
  }

  @Solution
  public void moreSprings(int extras) {
    numSprings += extras + 1;
  }

  public int getSprings() {
    return numSprings;
  }

  private void addSpringsFrom(BadParameterizedMethodAccessWidget other) {
    numSprings += other.numSprings;
  }

  @Generator
  public static BadParameterizedMethodAccessWidget generate(int complexity, Random random) {
    BadParameterizedMethodAccessWidget widget =
        new BadParameterizedMethodAccessWidget(random.nextInt(complexity + 1));
    widget.addSpringsFrom(widget);
    return widget;
  }
}
