package edu.illinois.cs.cs125.answerable.testing.fixtures.GeneratorMap;

import edu.illinois.cs.cs125.answerable.annotations.Generator;
import edu.illinois.cs.cs125.answerable.testing.library.Location;

import java.util.Random;

public class OverridesExternalGenerator {
  @Generator
  private static Location genLocation(int c, Random r) {
    return new Location(100, 100);
  }

  public static boolean isOrigin(Location loc) {
    return loc.getLongitude() == 0 && loc.getLatitude() == 0;
  }
}
