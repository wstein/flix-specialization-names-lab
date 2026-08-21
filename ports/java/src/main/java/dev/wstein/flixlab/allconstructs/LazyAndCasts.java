package dev.wstein.flixlab.allconstructs;

import java.util.function.Supplier;

/**
 * Section 15: Lazy Evaluation, Unsafe &amp; Checked/Unchecked Casts. Java has no first-class lazy
 * value; a memoizing {@link Supplier} is the idiomatic approximation of Flix's {@code lazy}/{@code
 * force}. See {@code package-info.java} for why {@code unsafe IO} has no equivalent to approximate
 * at all.
 */
public final class LazyAndCasts {
  private LazyAndCasts() {}

  private static final class Lazy<T> implements Supplier<T> {
    private final Supplier<T> underlying;
    private T value;
    private boolean computed;

    Lazy(Supplier<T> underlying) {
      this.underlying = underlying;
    }

    @Override
    public synchronized T get() {
      if (!computed) {
        value = underlying.get();
        computed = true;
      }
      return value;
    }
  }

  public static int lazyAndCastsDemo() {
    Lazy<Integer> lazyVal = new Lazy<>(() -> 10 + 20);
    int forcedVal = lazyVal.get();
    System.out.println("Evaluating (Java has no static effect tracking to bypass)");
    return forcedVal;
  }
}
