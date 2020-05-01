package examples.proxy;

public class WidgetArgumentWidget {

    private String myName;
    private int numSprings;

    public WidgetArgumentWidget(String setName, int setSprings) {
        myName = setName;
        numSprings = setSprings;
    }

    public int getSprings() {
        return numSprings;
    }

    public String getName() {
        return myName;
    }

    public void copyNameFrom(WidgetArgumentWidget other) {
        myName = other.myName;
    }

}
