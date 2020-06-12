package examples.proxy;

public class ComplexGeneratorWidget {

  private String[] springs;

  public String springSuffix = "";

  public ComplexGeneratorWidget(int setSprings) {
    springs = new String[setSprings];
  }

  public void createSprings(String prefix) {
    for (int i = 0; i < springs.length; i++) {
      springs[i] = prefix + i;
    }
  }

  public void applySuffix() {
    // Uses a public field so we can test proxying
    for (int i = 0; i < springs.length; i++) {
      springs[i] += springSuffix;
    }
  }

  public String replaceSpring(int index, String newValue) {
    String original = springs[index];
    springs[index] = newValue;
    return original;
  }

  public String getSpring(int index) {
    return springs[index];
  }
}
