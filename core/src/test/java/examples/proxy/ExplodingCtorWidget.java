package examples.proxy;

public class ExplodingCtorWidget {

    private String[] positions;

    public ExplodingCtorWidget() {
        throw new RuntimeException("Kaboom!");
    }

    public void positionSprings(int springs) {
        springs = Math.abs(springs);
        positions = new String[springs];
        for (int i = 0; i < springs; i++) {
            positions[i] = "Spring " + (i + 1);
        }
    }

    public String[] getSpringPositions() {
        return positions;
    }

}
