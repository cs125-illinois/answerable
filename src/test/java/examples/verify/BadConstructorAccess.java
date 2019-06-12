package examples.verify;

import edu.illinois.cs.cs125.answerable.Generator;

import java.util.Random;

public class BadConstructorAccess {

    private int things;

    BadConstructorAccess(int setThings) {
        things = setThings;
    }

    @Generator
    public static BadConstructorAccess generate(int complexity, Random random) {
        return new BadConstructorAccess(random.nextInt(complexity + 1));
    }

}
