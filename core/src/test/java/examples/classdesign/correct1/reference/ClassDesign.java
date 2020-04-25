package examples.classdesign.correct1.reference;

import edu.illinois.cs.cs125.answerable.Next;

import java.util.LinkedList;

public class ClassDesign extends LinkedList {
    public static int numGets = 0;

    @Override
    public Object get(int index) {
        numGets++;
        return super.get(index);
    }

    @Next
    public static ClassDesign next(ClassDesign current, int iter) {
        return null;
    }
}
