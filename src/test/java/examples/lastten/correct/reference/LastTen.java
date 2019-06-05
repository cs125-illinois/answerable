package examples.lastten.correct.reference;

import edu.illinois.cs.cs125.answerable.Next;
import edu.illinois.cs.cs125.answerable.Solution;
import edu.illinois.cs.cs125.answerable.TestOutput;
import edu.illinois.cs.cs125.answerable.Verify;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;


/* A more complex problem that combines class design with a custom comparison function. The @Solution annotation on the
 * class indicates that class design should be checked: correct1 public functions and signatures, correct1 count of
 * public variables (none), inheritance, etc.
 *
 * An open question here is how we handle object reuse. This example tries out a few ideas. One is a argument to the
 * class-level @Solution annotation. This says to reuse examples.lastten.correct.LastTen objects 32 times during testing before recreating one.
 * Of course for examples.lastten.correct.LastTen this has to be larger than 10!
 */
public class LastTen {
    private int[] values = new int[10];
    private int currentIndex = 0;

    /* During each test iteration we provide a random input to add. This should be included in the class design check.
     * We use @Input rather than @Solution
     * since this function produces no output to the console either, but rather
     * effects internal state in a way that we need to compare later. */


    @Solution
    public void add(int value) {
        values[currentIndex] = value;
        currentIndex = (currentIndex + 1) % 10;
    }

    public int[] values() {
        return values;
    }

    /* But then we need to use a custom comparison. This should be ignored in the class design check, but is used
     * during testing to determine whether the last add succeeded or not.
     */


    @Verify
    public static void verify(TestOutput<LastTen> ours, TestOutput<LastTen> theirs) {
        int[] ourValues = ours.getReceiver().values();
        int[] theirValues = theirs.getReceiver().values();
        Arrays.sort(ourValues);
        Arrays.sort(theirValues);
        assertArrayEquals(ourValues, theirValues);
    }

    /* Here's another idea for how to control object reuse. @Next marks a function that gets called each time during
     * our testing loop. It's passed the current object and the number of iterations we've completed and
     * should return an object to use in the next loop.
     *
     * This is more flexible, since the code can create an entirely new object, return the existing object, or even
     * make other changes using the object's public API. (Like adding nodes to the tree, for example.)
     */


    @Next
    public static LastTen next(LastTen current, int iteration, Random random) {
        if (iteration % 32 == 0 || current == null) {
            return new LastTen();
        } else {
            return current;
        }
    }
}
