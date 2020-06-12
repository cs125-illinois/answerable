package examples.classdesign.correct1.reference;

import edu.illinois.cs.cs125.answerable.annotations.Next;
import java.util.LinkedList;
import java.util.Random;

public class ClassDesign extends LinkedList {
  public static int numGets = 0;

  @Override
  public Object get(int index) {
    numGets++;
    return super.get(index);
  }

  @Next
  public static ClassDesign next(ClassDesign current, int iter, Random random) {
    return null;
  }
}
