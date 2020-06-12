package examples.sorting;

public class ArraySorter {

  public static int[] sort(int[] arr) {
    boolean swapped = true;
    while (swapped) {
      swapped = false;
      for (int i = 1; i < arr.length; i++) {
        if (arr[i - 1] > arr[i]) {
          swapped = true;
          int temp = arr[i];
          arr[i] = arr[i - 1];
          arr[i - 1] = temp;
        }
      }
    }
    return arr;
  }
}
