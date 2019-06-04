package examples.proxy;

public class Widget {

    private String[] positions;

    public void positionSprings(int springs) {
        positions = new String[springs];
        for (int i = 0; i < springs; i++) {
            positions[i] = "Spring " + (i + 1);
        }
    }

    public String[] getSpringPositions() {
        return positions;
    }

}
