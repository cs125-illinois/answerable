package examples.testgeneration.generators.reference;

import edu.illinois.cs.cs125.answerable.Generator;
import edu.illinois.cs.cs125.answerable.Solution;

import java.util.Random;

public class OverrideDefaultArray {

    @Solution
    public static String test(String[][] strings) {
        StringBuilder sb = new StringBuilder();

        for(String[] arr : strings) {
            for(String s : arr) {
                sb.append(s);
            }
        }

        return sb.toString();
    }

    @Generator
    public static String[][] generateStrings(int complexity, Random random) {
        return new String[][] { { "Hello" }, { "generator", "world!" } };
    }

}
