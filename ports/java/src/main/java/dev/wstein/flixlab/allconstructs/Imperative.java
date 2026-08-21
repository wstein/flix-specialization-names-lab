package dev.wstein.flixlab.allconstructs;

/**
 * Section 14: Imperative Constructs, Foreach Loops &amp; References in Regions. Flix's {@code Ref}
 * cell in a region becomes a plain local variable -- the same block-scoping approximation as
 * section 5's {@code Person}.
 */
public final class Imperative {
  private Imperative() {}

  public static int imperativeDemo() {
    int total = 0;
    int[] arr = {1, 2, 3, 4, 5};

    for (int item : arr) {
      total += item;
    }

    for (int item : new int[] {10, 20, 30}) {
      if (item > 15) {
        total += item;
      }
    }

    return total;
  }
}
