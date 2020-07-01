package edu.illinois.cs.cs125.answerable.testing.fixtures.GeneratorMap;

import edu.illinois.cs.cs125.answerable.testing.library.Location;

public class UsesExternalGenerator {
  public static boolean isOrigin(Location loc) {
    return loc.getLatitude() == 0 && loc.getLongitude() == 0;
  }
}
