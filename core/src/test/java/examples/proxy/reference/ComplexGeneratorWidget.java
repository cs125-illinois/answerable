package examples.proxy.reference;

import edu.illinois.cs.cs125.answerable.annotations.Generator;
import edu.illinois.cs.cs125.answerable.annotations.Helper;
import edu.illinois.cs.cs125.answerable.annotations.Solution;
import edu.illinois.cs.cs125.answerable.annotations.Verify;
import edu.illinois.cs.cs125.answerable.api.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Assertions;

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

  @Solution
  public String getSpring(int index) {
    return springs[index];
  }

  @Verify
  public static void verify(
      TestOutput<ComplexGeneratorWidget> ours, TestOutput<ComplexGeneratorWidget> theirs) {
    int index = 0;
    while (true) {
      String ourSpring;
      try {
        ourSpring = ours.getReceiver().getSpring(index);
      } catch (Throwable t) {
        break;
      }
      Assertions.assertEquals(ourSpring, theirs.getReceiver().getSpring(index));
      index++;
    }
    if (index > 0) {
      ours.getReceiver().springSuffix = "!";
      ours.getReceiver().applySuffix();
      theirs.getReceiver().springSuffix = "!";
      theirs.getReceiver().applySuffix();
      Assertions.assertEquals(ours.getReceiver().getSpring(0), theirs.getReceiver().getSpring(0));
    }
  }

  @Generator
  public static ComplexGeneratorWidget generate(int complexity, Random random) {
    ComplexGeneratorWidget widget;
    if (complexity == 0) {
      return new ComplexGeneratorWidget(0);
    } else if (complexity % 4 == 0) {
      widget = new ComplexGeneratorWidget(random.nextInt(complexity));
      widget.createSprings("Spring ");
    } else if (complexity % 4 == 1) {
      widget = new ComplexGeneratorWidget(random.nextInt(complexity) + 1);
      widget.createSprings("#");
      widget.replaceSpring(0, "Gone");
      return widget;
    } else if (complexity % 4 == 2) {
      widget = new ComplexGeneratorWidget(17);
      widget.createSprings("Spring #");
      for (int i = 0; i < 17; i++) {
        if (i % 3 == 0 || i % 7 == 1) {
          try {
            replaceMultipleSprings(widget, i, i + 1);
          } catch (Exception e) {
            // Do nothing
          }
        }
      }
    } else {
      widget = listToArray(generateThree(complexity, random))[random.nextInt(3)];
    }
    return widget;
  }

  @Helper
  private static void replaceMultipleSprings(ComplexGeneratorWidget widget, int... toClobber) {
    for (int index : toClobber) {
      widget.replaceSpring(index, "Broken");
    }
  }

  @Helper
  private static List<ComplexGeneratorWidget> generateThree(int complexity, Random random) {
    List<ComplexGeneratorWidget> options = new ArrayList<>();
    options.add(generate(complexity - 1, random));
    options.add(generate(complexity - 2, random));
    options.add(generate(complexity - 3, random));
    return options;
  }

  @Helper
  private static ComplexGeneratorWidget[] listToArray(List<ComplexGeneratorWidget> options) {
    return options.toArray(new ComplexGeneratorWidget[3]);
  }
}
