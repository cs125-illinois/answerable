package examples.testgeneration.mutatestaticfield.another;

public class Counter {

    private static int clicks;

    public static int increment() {
        clicks += 2;
        clicks--;
        return clicks;
    }

}
