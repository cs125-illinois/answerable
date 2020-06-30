package examples.noreceiver.single.parameterfield;

import edu.illinois.cs.cs125.answerable.core.Simple;
import edu.illinois.cs.cs125.answerable.core.Two;

import java.util.Arrays;
import java.util.List;

public class Correct {
  @Simple
  public static List<Two<Integer, Integer>> simple = Arrays.asList(new Two<>(88, 8888), new Two<>(8888, 88));

  public static boolean value(int first, int second) {
    return first == 88 && second == 8888;
  }
}
