package examples.proxy.reference;

import edu.illinois.cs.cs125.answerable.api.*;
import org.junit.jupiter.api.Assertions;

import java.util.Random;

public class FieldLinkedList {

    public FieldLinkedList next;
    public String value;

    public FieldLinkedList(String setValue) {
        value = setValue;
    }

    public void populateNext(String setNextValue) {
        next = new FieldLinkedList(setNextValue);
    }

    @Verify(standalone = true)
    public static void verify(TestOutput<FieldLinkedList> ours, TestOutput<FieldLinkedList> theirs, Random random) {
        String nextStr = Generators.defaultAsciiStringGenerator(10, random);
        theirs.getReceiver().populateNext(nextStr);
        Assertions.assertEquals(theirs.getReceiver().next.value, nextStr);
    }

    @Generator
    public static FieldLinkedList generate(int complexity, Random random) {
        return new FieldLinkedList(Generators.defaultAsciiStringGenerator(complexity, random));
    }

}
