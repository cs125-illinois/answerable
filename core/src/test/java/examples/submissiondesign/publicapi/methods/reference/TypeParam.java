package examples.submissiondesign.publicapi.methods.reference;

public class TypeParam {
  public final <T> T foo(String s, T t) {
    return t;
  }
}
