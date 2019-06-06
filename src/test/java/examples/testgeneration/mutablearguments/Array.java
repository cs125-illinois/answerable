package examples.testgeneration.mutablearguments;

public class Array {

    public static void mutate(int[] array) {
        for (int i = 0; i < array.length; i++) {
            array[i]--;
        }
    }

}
