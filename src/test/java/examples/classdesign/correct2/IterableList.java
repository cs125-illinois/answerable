package examples.classdesign.correct2;

import java.util.Iterator;
import java.util.List;

public interface IterableList extends Iterator, List {
    @Override
    public boolean add(Object o);

    @Override
    public Object next();
}
