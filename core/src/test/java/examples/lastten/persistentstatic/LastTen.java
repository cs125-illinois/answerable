package examples.lastten.persistentstatic;

public class LastTen {
    private static int[] values = new int[10];
    private static int currentIndex = 0;

    public void add(int value) {
        values[currentIndex] = value;
        currentIndex = (currentIndex + 1) % 10;
    }

    public int[] values() {
        return values;
    }
}
