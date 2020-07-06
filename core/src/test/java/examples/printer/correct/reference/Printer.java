package examples.printer.correct.reference;

import edu.illinois.cs.cs125.answerable.annotations.Solution;

public class Printer {
  /* Because this function is static and void answerable should determine that it must produce output. So record
   * both System.out and System.err and compare against the solution. */
  @Solution(prints = true)
  public static void printHello() {
    System.out.println("Hello, world!");
  }
}
