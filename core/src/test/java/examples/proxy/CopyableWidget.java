package examples.proxy;

public class CopyableWidget {

  private int springs;

  public CopyableWidget(int setSprings) {
    springs = setSprings;
  }

  public CopyableWidget copy() {
    CopyableWidget dup = new CopyableWidget(0);
    dup.springs = springs;
    return dup;
  }

  public String checkEquivalence(CopyableWidget other) {
    if (other == null) return "null";
    if (other == this) return "reference-equal";
    if (other.springs == springs) return "value-equal";
    return "different";
  }
}
