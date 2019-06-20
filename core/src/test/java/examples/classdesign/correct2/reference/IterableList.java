package examples.classdesign.correct2.reference;

import edu.illinois.cs.cs125.answerable.api.Next;

import java.util.Iterator;
import java.util.List;

public interface IterableList extends Iterator, List {
    @Override
    public boolean add(Object o);

    @Override
    public Object next();

    @Next
    public IterableList getNext(IterableList current, int iter);
}
