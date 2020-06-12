package examples.proxy;

public class StaticInitGeneratorWidget {

  private int numSprings;

  public StaticInitGeneratorWidget(int springs) {
    numSprings = springs;
  }

  public void moreSprings(int extras) {
    numSprings += extras + 1;
  }

  public int getSprings() {
    return numSprings;
  }
}
