package examples.submissiondesign.correct1;

import java.util.LinkedList;

public class ClassDesign extends LinkedList {
  public static int numGets = 0;

  @Override
  public Object get(int index) {
    numGets++;
    return super.get(index);
  }
}
