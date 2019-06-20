package examples.testgeneration.mutablearguments.reference;

import edu.illinois.cs.cs125.answerable.api.Solution;

public class Array {

    @Solution
    public static void mutate(int[] array) {
        for (int i = 0; i < array.length; i++) {
            array[i]--;
        }
    }

}
