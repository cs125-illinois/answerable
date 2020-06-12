package examples.testgeneration.arrays;

public class IntArrayArrayParameter {

  public static int sum(int[][] matrix) {
    int sum = 0;
    for (int[] r : matrix) {
      for (int n : r) {
        sum += n;
      }
    }
    return sum;
  }
}
