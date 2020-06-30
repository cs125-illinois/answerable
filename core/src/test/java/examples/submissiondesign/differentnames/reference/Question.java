package examples.submissiondesign.differentnames.reference;

import edu.illinois.cs.cs125.answerable.annotations.Generator;
import edu.illinois.cs.cs125.answerable.annotations.Solution;
import java.util.Random;

public class Question {
  @Solution
  public int add(int x, int y) {
    return x + y;
  }

  @Generator
  private static Question genq(int c, Random r) {
    return new Question();
  }
}
