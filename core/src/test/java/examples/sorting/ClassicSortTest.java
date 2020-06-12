package examples.sorting;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Assertions;

public class ClassicSortTest {

  public static void test() {
    Random random1 = new Random(0x0403);
    Random random2 = new Random(0x0403);
    for (int i = 0; i < 1024; i++) {
      int[] toSort1 = randomArray(random1, i);
      int[] toSort2 = randomArray(random2, i);
      ArraySorter.sort(toSort1);
      Arrays.sort(toSort2);
      Assertions.assertArrayEquals(toSort2, toSort1);
    }
  }

  private static int[] randomArray(Random random, int bound) {
    int[] arr = new int[random.nextInt(bound + 1)];
    for (int i = 0; i < arr.length; i++) arr[i] = random.nextInt(1024);
    return arr;
  }
}
