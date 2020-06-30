package examples.noreceiver.single.simplecaseoverride;

import edu.illinois.cs.cs125.answerable.core.Simple;

public class Correct {
  @Simple
  public final static int[] simple = new int[]{8888, 888888};

  public static boolean value(int first) {
    return first == 8888 || first == 888888;
  }
}
