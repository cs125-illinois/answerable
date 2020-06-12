package examples.proxy;

public class SubclassedInnerClassWidget {

  public void doNothing(int widgets) {
    // Does nothing
  }

  public Object getInner(int setWidgets) {
    return new NamedInner() {
      {
        this.widgets = setWidgets;
      }
    };
  }

  public class NamedInner {
    int widgets;

    public int getWidgets() {
      return widgets;
    }
  }
}
