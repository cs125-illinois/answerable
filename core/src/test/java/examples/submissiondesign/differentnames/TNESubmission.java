package examples.submissiondesign.differentnames;

import java.util.List;
import org.jetbrains.annotations.NotNull;

public class TNESubmission implements Comparable<TNESubmission> {

  public TNESubmission unused;

  @Override
  public int compareTo(@NotNull TNESubmission o) {
    return 0;
  }

  public List<TNESubmission> singletonList() {
    return List.of(new TNESubmission());
  }

  static class Companion {
    public static TNESubmission factory() {
      return new TNESubmission();
    }
  }
}
