package dev.wstein.flixlab.allconstructs;

import java.util.Comparator;
import java.util.function.Supplier;

/**
 * Section 16: Specialization Coverage -- anonymous classes. Java's anonymous-class syntax is the
 * exact same construct Flix's demo uses -- a direct, unapproximated mapping.
 */
public final class AnonClass {
  private AnonClass() {}

  public static String anonClassDemo() {
    Supplier<String> supplier =
        new Supplier<String>() {
          @Override
          public String get() {
            return "from-anon-class";
          }
        };
    Comparator<String> cmp =
        new Comparator<String>() {
          @Override
          public int compare(String a, String b) {
            return a.length() - b.length();
          }
        };
    String v = supplier.get();
    int c = cmp.compare("aa", "b");
    return v + "/" + c;
  }
}
