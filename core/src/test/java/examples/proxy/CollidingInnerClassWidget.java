package examples.proxy;

public class CollidingInnerClassWidget {

    public void doNothing(int widgets) {
        // Does nothing
    }

    public Object getInner(int setWidgets) {
        NamedInner inner = new CollidingInner();
        inner.widgets = setWidgets;
        return inner;
    }

    public class NamedInner {
        int widgets;
        public int getWidgets() {
            return widgets;
        }
    }

    private class CollidingInner extends NamedInner { }

}
