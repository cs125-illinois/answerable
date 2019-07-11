package examples.testgeneration.mutatestaticfield.timesout;

public class Counter {

    private static int count;

    public static int increment() {
        count++;
        if (count >= 20) {
            while (true) {}
        }
        return count;
    }

}
