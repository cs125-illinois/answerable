package edu.illinois.cs.cs125.answerable.testing.library;

import edu.illinois.cs.cs125.answerable.annotations.Generator;
import edu.illinois.cs.cs125.answerable.api.Generators;

import java.util.Random;

public class Location {
  private double longitude;
  private double latitude;

  public Location(double setLongitude, double setLatitude) {
    longitude = setLongitude;
    latitude = setLatitude;
  }

  public double getLongitude() {
    return longitude;
  }

  public double getLatitude() {
    return latitude;
  }

  @Generator
  private static Location generate(int c, Random r) {
    return new Location(
      Generators.defaultDoubleGenerator(c, r),
      Generators.defaultDoubleGenerator(c, r)
    );
  }
}
