package examples.proxy;

public class HelperWidget {

  private int springs;

  public void positionSprings(int numSprings) {
    springs = Math.abs(numSprings);
  }

  public String[] getSpringPositions() {
    String[] positions = new String[springs];
    for (int i = 0; i < springs; i++) {
      positions[i] = "Spring " + (i + 1);
    }
    return positions;
  }
}
