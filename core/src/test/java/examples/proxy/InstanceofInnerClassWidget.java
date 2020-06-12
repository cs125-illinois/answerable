package examples.proxy;

public class InstanceofInnerClassWidget {

  public void doNothing(int widgets) {
    // Does nothing
  }

  public Object getInner(int widgets) {
    NamedInner inner = new NamedInner();
    inner.widgets = widgets;
    return inner;
  }

  public class NamedInner {
    private int widgets;

    public int getWidgets() {
      return widgets;
    }
  }
}
