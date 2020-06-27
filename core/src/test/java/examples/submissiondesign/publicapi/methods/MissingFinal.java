package examples.submissiondesign.publicapi.methods;

public class MissingFinal {
  public <T> T foo(String s, T t) {
    return t;
  }
}
