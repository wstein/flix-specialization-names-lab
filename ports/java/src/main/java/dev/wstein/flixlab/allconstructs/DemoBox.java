package dev.wstein.flixlab.allconstructs;

import java.util.List;
import java.util.Optional;

/**
 * Section 16: Specialization Coverage -- a polymorphic struct.
 *
 * <p>In Flix, {@code DemoBox[a, r]} specialized at ten distinct element types mints ten distinct
 * generated classes. On the JVM, generics erase: {@code DemoBox<A>} compiles to exactly
 * <em>one</em> class regardless of how many types it's instantiated at below, and every call reuses
 * that same class at runtime. This is the comparison's headline point -- see docs/ports.md.
 */
public final class DemoBox<A> {
  private A value;

  public DemoBox(A value) {
    this.value = value;
  }

  public A getValue() {
    return value;
  }

  public void setValue(A value) {
    this.value = value;
  }

  public record Pair<A, B>(A first, B second) {}

  public static <A> A roundTrip(A v) {
    DemoBox<A> b = new DemoBox<>(v);
    b.setValue(v);
    return b.getValue();
  }

  public static String boxDemo() {
    int i = roundTrip(42);
    String s = roundTrip("boxed");
    boolean b = roundTrip(true);
    char c = roundTrip('x');
    double f = roundTrip(2.5);
    long l = roundTrip(7L);
    Color col = roundTrip(Color.BLUE);
    Optional<Integer> opt = roundTrip(Optional.of(1));
    List<String> lst = roundTrip(List.of("a", "b"));
    Pair<Integer, String> tup = roundTrip(new Pair<>(1, "t"));
    return i
        + "/"
        + s
        + "/"
        + b
        + "/"
        + c
        + "/"
        + f
        + "/"
        + l
        + "/"
        + col
        + "/"
        + opt
        + "/"
        + lst.size()
        + "/"
        + tup.first();
  }
}
