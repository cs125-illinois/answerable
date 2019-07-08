package examples.testgeneration.arrays.reference;

import edu.illinois.cs.cs125.answerable.api.Solution;

public class IntArrayArrayParameter {

    @Solution
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
