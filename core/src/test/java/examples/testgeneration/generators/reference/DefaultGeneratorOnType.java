package examples.testgeneration.generators.reference;

import edu.illinois.cs.cs125.answerable.annotations.Solution;
import examples.testgeneration.generators.common.Location;

public class DefaultGeneratorOnType {
  @Solution
  public boolean isAtLocation(Location location) {
    if (location.getLatitude() == 0.01 && location.getLongitude() == 0.02) {
      return true;
    } else {
      return false;
    }
  }
}
