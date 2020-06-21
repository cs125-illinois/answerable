package examples.testgeneration.generators.common;

import edu.illinois.cs.cs125.answerable.annotations.Generator;
import java.util.Random;

public class Location {
  private double latitude;
  private double longitude;

  public Location(final double setLatitude, final double setLongitude) {
    latitude = setLatitude;
    longitude = setLongitude;
  }

  public double getLatitude() {
    return latitude;
  }

  public double getLongitude() {
    return longitude;
  }

  @Generator
  public static Location generateLocation(int complexity, Random random) {
    return new Location(random.nextDouble(), random.nextDouble());
  }
}
