package examples.submissiondesign.publicapi.methods;

public class TypeParam {
  public final <T> T foo(String s, T t) {
    return t;
  }
}
