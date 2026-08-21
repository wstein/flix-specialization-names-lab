package dev.wstein.flixlab.allconstructs;

import java.util.function.Function;
import java.util.function.IntUnaryOperator;

/**
 * Section 9: Functions, Currying, Pipelines, Lambdas &amp; Tail Recursion. Java has no native pipe
 * operator, unlike Flix's {@code |>} -- {@link #pipe} is a small hand-rolled stand-in, and {@code
 * ||>} becomes a direct call. Java also gives no tail-call guarantee (no {@code @tailrec} the way
 * Scala has); at this call depth (5) it does not matter, but it is a real gap, not an oversight.
 */
public final class Functions {
  private Functions() {}

  public static int add(int x, int y) {
    return x + y;
  }

  public static IntUnaryOperator curriedMultiply(int x) {
    return y -> x * y;
  }

  public static <A> A genericIdentity(A x) {
    return x;
  }

  public static int higherOrderDemo(IntUnaryOperator f, int x) {
    return f.applyAsInt(x);
  }

  public static <A, B> B pipe(A value, Function<A, B> f) {
    return f.apply(value);
  }

  public static int pipelineDemo(int x) {
    return pipe(pipe(x, n -> n + 5), n -> n * 2);
  }

  public static int binaryPipelineDemo(int a, int b) {
    return add(a, b);
  }

  public static long factorial(long n, long acc) {
    return n <= 1 ? acc : factorial(n - 1, n * acc);
  }
}
