package examples.testgeneration.generators.defaults;

public class DefaultCtor {

  private int count = 100;

  public int accumulate(int n) {
    count += n;
    return count;
  }
}
