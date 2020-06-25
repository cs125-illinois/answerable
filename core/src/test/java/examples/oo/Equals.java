package examples.oo;

public class Equals {
  private int value;

  public Equals(int setValue) {
    value = setValue;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || !(o instanceof Equals)) {
      return false;
    }
    Equals other = (Equals) o;
    return value == other.value;
  }
}
