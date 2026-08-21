package dev.wstein.flixlab.allconstructs;

/**
 * Section 3: a hand-rolled two-case result type, distinct from any standard library {@code Result}.
 */
public sealed interface SimpleResult<T, E> permits SimpleResult.Ok, SimpleResult.Err {
  record Ok<T, E>(T value) implements SimpleResult<T, E> {}

  record Err<T, E>(E error) implements SimpleResult<T, E> {}
}
