package examples.testgeneration.standaloneverify;

public class MutatesArguments {
  public static int[] soln(int[] ints) {
    if (ints.length > 0) {
      ints[0] = 100;
    }
    return ints;
  }
}
