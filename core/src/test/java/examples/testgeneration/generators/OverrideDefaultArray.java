package examples.testgeneration.generators;

public class OverrideDefaultArray {

  public static String test(String[][] ss) {
    String out = "";

    for (String[] arr : ss) {
      for (String s : arr) {
        out += s;
      }
    }

    return out;
  }
}
