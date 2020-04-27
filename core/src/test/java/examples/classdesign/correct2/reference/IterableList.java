package examples.classdesign.correct2.reference;

import edu.illinois.cs.cs125.answerable.annotations.Next;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

public interface IterableList extends Iterator, List {
    @Override
    public boolean add(Object o);

    @Override
    public Object next();

    @Next
    public static IterableList getNext(IterableList current, int iter, Random random) {
        return null;
    }
}
