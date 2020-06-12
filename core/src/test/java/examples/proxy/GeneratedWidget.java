package examples.proxy;

public class GeneratedWidget {

  private int m_springs;

  public GeneratedWidget(int springs) {
    m_springs = springs;
  }

  public void moreSprings(int extras) {
    m_springs += extras + 1;
  }

  public int getSprings() {
    return m_springs;
  }
}
