package edu.illinois.cs.cs125.answerable.testing.fixtures.oo;

public class GetSetEquals {
  private int value;

  public int getValue() {
    return value;
  }

  public GetSetEquals(int setValue) {
    value = setValue;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof GetSetEquals)) {
      return false;
    }
    GetSetEquals other = (GetSetEquals) o;
    return false;
  }
}
