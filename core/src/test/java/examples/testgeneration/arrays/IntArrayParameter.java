package examples.testgeneration.arrays;

public class IntArrayParameter {

    public static int sum(final int[] numbers) {
        int sum = 0;
        for (int n : numbers) {
            sum += n;
        }
        return sum;
    }

}
