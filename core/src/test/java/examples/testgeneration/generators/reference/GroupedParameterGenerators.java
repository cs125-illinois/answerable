package examples.testgeneration.generators.reference;

import edu.illinois.cs.cs125.answerable.annotations.Generator;
import edu.illinois.cs.cs125.answerable.annotations.Pair;
import edu.illinois.cs.cs125.answerable.annotations.Solution;
import java.util.Random;

public class GroupedParameterGenerators {
  @Solution
  public boolean atLocation(double latitude, double longitude) {
    return latitude == 8.8888 && longitude == 9.9999;
  }

  @Generator
  public static Pair<Double, Double> locationGenerator(int complexity, Random random) {
    if (complexity == 0) {
      return new Pair<>(8.8888, 9.9999);
    } else {
      return new Pair<>(random.nextDouble(), random.nextDouble());
    }
  }
}
