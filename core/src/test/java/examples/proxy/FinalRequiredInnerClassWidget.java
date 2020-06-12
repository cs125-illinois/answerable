package examples.proxy;

public final class FinalRequiredInnerClassWidget {

  public final void doNothing(int widgets) {
    // Does nothing
  }

  public final NamedInner getInner(int widgets) {
    NamedInner inner = new NamedInner();
    inner.widgets = widgets;
    return inner;
  }

  public final class NamedInner {
    private int widgets;

    public final int getWidgets() {
      return widgets;
    }
  }
}
