package examples.submissiondesign.differentnames.reference;

import java.util.List;
import org.jetbrains.annotations.NotNull;

public class TypeNameEverywhere implements Comparable<TypeNameEverywhere> {

  public TypeNameEverywhere unused;

  @Override
  public int compareTo(@NotNull TypeNameEverywhere o) {
    return 0;
  }

  public List<TypeNameEverywhere> singletonList() {
    return List.of(new TypeNameEverywhere());
  }

  static class Companion {
    public static TypeNameEverywhere factory() {
      return new TypeNameEverywhere();
    }
  }
}
