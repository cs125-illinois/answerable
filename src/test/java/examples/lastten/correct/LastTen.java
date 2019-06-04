package examples.lastten.correct;

public class LastTen {
    private int[] values = new int[10];
    private int currentIndex = 0;

    public void add(int value) {
        values[currentIndex] = value;
        currentIndex = (currentIndex + 1) % 10;
    }

    public int[] values() {
        return values;
    }
}
