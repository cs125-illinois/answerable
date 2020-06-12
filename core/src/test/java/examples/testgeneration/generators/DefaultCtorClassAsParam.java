package examples.testgeneration.generators;

public class DefaultCtorClassAsParam {

  public void accept(DefaultCtorClassAsParam other) {
    System.out.println(other == null ? "Null" : "OK");
  }
}
