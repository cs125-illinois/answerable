package examples.testgeneration.generators;

public class StaticTakesInstance {

    public static void accept(StaticTakesInstance inst) {
        if (inst != null) {
            System.out.println("OK");
        }
    }

}
