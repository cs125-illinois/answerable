package examples.testgeneration.arrays.reference;

import edu.illinois.cs.cs125.answerable.Solution;

public class IntArrayParameter {

    @Solution
    public static int sum(final int[] numbers) {
        int sum = 0;
        for (int n : numbers) {
            sum += n;
        }
        return sum;
    }

}
