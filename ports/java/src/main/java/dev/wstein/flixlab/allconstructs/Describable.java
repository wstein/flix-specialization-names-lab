package dev.wstein.flixlab.allconstructs;

import java.util.Optional;

/**
 * Section 4: Traits, Instances &amp; Trait Constraints, approximated with dictionary passing: Java
 * has no native typeclass mechanism, so each "instance" is a plain implementation of this
 * interface, passed to {@link #describeItem} explicitly rather than resolved implicitly the way
 * Flix's {@code with Describable[a]} constraint is.
 */
public interface Describable<T> {
  String describe(T x);

  default String describeTwice(T x) {
    return describe(x) + " & " + describe(x);
  }

  static Describable<Integer> forInt() {
    return x -> "Int(" + x + ")";
  }

  static Describable<String> forString() {
    return x -> "String(" + x + ")";
  }

  static Describable<Color> forColor() {
    return x ->
        switch (x) {
          case RED -> "Color.Red";
          case GREEN -> "Color.Green";
          case BLUE -> "Color.Blue";
        };
  }

  /**
   * Conditional instance: {@code Optional<T>} is describable whenever {@code T} is, forwarding to
   * the inner value's instance.
   */
  static <T> Describable<Optional<T>> forOptional(Describable<T> inner) {
    return opt -> opt.map(v -> "Some(" + inner.describe(v) + ")").orElse("None");
  }

  static <T> String describeItem(T x, Describable<T> instance) {
    return instance.describeTwice(x);
  }
}
