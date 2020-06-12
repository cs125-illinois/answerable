package examples.testgeneration.mutatestaticfield;

public class Counter {

  private static int number;

  public static int increment() {
    number++;
    return number;
  }
}
