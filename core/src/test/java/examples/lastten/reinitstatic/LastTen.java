package examples.lastten.reinitstatic;

public class LastTen {
  private static int[] values;
  private static int currentIndex;

  public LastTen() {
    values = new int[10];
    currentIndex = 0;
  }

  public void add(int value) {
    values[currentIndex] = value;
    currentIndex = (currentIndex + 1) % 10;
  }

  public int[] values() {
    return values;
  }
}
