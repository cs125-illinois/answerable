package examples.testgeneration.generators.defaults.reference;

import edu.illinois.cs.cs125.answerable.annotations.Solution;

import java.util.Arrays;

public class MultiDimensionalPrimitiveArrays {

    @Solution(prints = true)
    public static void test(int[][] ints) {
        System.out.println(Arrays.deepToString(ints));
    }

}
