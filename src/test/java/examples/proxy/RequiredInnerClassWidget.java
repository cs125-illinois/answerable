package examples.proxy;

public class RequiredInnerClassWidget {

    public void doNothing(int widgets) {
        // Does nothing
    }

    public NamedInner getInner(int widgets) {
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
