package dev.wstein.flixlab.allconstructs;

import java.util.function.IntUnaryOperator;

/**
 * Section 16: Specialization Coverage -- lifted closures.
 *
 * <p>Both closures capture {@code n}; on the JVM they simply become instances of a synthetic lambda
 * class, same as any other lambda in Java -- there is no separate "specialization" step to observe
 * here the way there is in Flix's lambda-lifting pass.
 */
public final class LiftedClosures {
  private LiftedClosures() {}

  public record Closures(IntUnaryOperator f, IntUnaryOperator g) {}

  public static Closures liftedClosures(int n) {
    IntUnaryOperator f = x -> x + n;
    IntUnaryOperator g = y -> y * n;
    return new Closures(f, g);
  }

  public static int liftedClosuresDemo() {
    Closures closures = liftedClosures(3);
    return closures.f().applyAsInt(10) + closures.g().applyAsInt(10);
  }
}
