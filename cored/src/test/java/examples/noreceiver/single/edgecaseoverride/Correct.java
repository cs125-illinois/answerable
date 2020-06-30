package examples.noreceiver.single.edgecaseoverride;

import edu.illinois.cs.cs125.answerable.core.Edge;

public class Correct {
  @Edge
  public final static int[] edge = new int[]{8888, 888888};

  public static boolean value(int first) {
    return first == 8888 || first == 888888;
  }
}
