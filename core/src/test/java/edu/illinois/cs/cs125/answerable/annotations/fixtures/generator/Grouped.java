package edu.illinois.cs.cs125.answerable.annotations.fixtures.generator;

import edu.illinois.cs.cs125.answerable.annotations.*;
import java.util.Random;

public class Grouped {
  @Solution(name = "booleanByte")
  public static boolean booleanByte(boolean first, byte second) {
    return true;
  }

  @Generator(name = "booleanByte")
  public static Pair<Boolean, Byte> generateBooleanByte(int complexity, Random random) {
    return new Pair<>(true, (byte) 8);
  }

  @Solution(name = "charFloatInt")
  public static boolean charFloatInt(char first, float second, int third) {
    return true;
  }

  @Generator(name = "charFloatInt")
  public static Triple<Character, Float, Integer> generateCharFloatInt(
      int complexity, Random random) {
    return new Triple<>('8', 0.8f, 8);
  }

  @Solution(name = "intLongShortDouble")
  public static boolean intLongShortDouble(int first, long second, short third, double fourth) {
    return true;
  }

  @Generator(name = "intLongShortDouble")
  public static Quad<Integer, Long, Short, Double> generateIntLongShortDouble(
      int complexity, Random random) {
    return new Quad<>(8, 88888888L, (short) 88, 8.8);
  }

  @Solution(name = "intArrayDoubleArray")
  public static boolean intArrayDoubleArray(int[] first, double[] second) {
    return true;
  }

  @Generator(name = "intArrayDoubleArray")
  public static Pair<Integer[], Double[]> generateIntArrayDoubleArray(
      int complexity, Random random) {
    return new Pair<>(new Integer[] {8, 8}, new Double[] {8.8, 8.8});
  }

  @Solution(name = "intDoubleFloatArray")
  public static boolean intArrayDoubleFloatArray(int[] first, float[][] second) {
    return true;
  }

  @Generator(name = "intDoubleFloatArray")
  public static Pair<Integer[], Float[][]> generateIntArrayDoublFloatArray(
      int complexity, Random random) {
    return new Pair<>(new Integer[] {8, 8}, new Float[][] {{8.8f, 8.8f}});
  }
}
