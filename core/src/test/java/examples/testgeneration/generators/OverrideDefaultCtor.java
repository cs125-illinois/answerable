package examples.testgeneration.generators;

public class OverrideDefaultCtor {

  private int springs = 0;

  public OverrideDefaultCtor() {
    throw new ThreadDeath();
  }

  public OverrideDefaultCtor(int unused) {
    // Do nothing
  }

  public int getSprings() {
    return springs;
  }
}
