package examples.testgeneration.generators.reference;

import edu.illinois.cs.cs125.answerable.api.Generator;
import edu.illinois.cs.cs125.answerable.api.Solution;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class WildcardTypes {
    @Solution
    public String callToString(List<? extends Object> any) {
        return any.toString();
    }

    @Generator
    public static Object mkObject(int comp, Random r) { return new Object(); }

    @Generator
    public static List<Object> mkList(int comp, Random r) {
        int length = r.nextInt(comp);
        return IntStream.range(0, length).mapToObj(unused -> mkObject(comp, r)).collect(Collectors.toList());
    }
}
