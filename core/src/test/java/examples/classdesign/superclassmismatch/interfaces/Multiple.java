package examples.classdesign.superclassmismatch.interfaces;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public interface Multiple extends Iterator, Function, List {}
