package dev.wstein.flixlab.allconstructs;

/**
 * Section 3: a parametric, recursive algebraic data type. A sealed interface plus record cases is
 * Java's native equivalent of a Flix {@code enum} -- exhaustiveness-checked by the compiler at
 * every {@code switch}.
 */
public sealed interface Tree<T> permits Tree.Leaf, Tree.Node {
  record Leaf<T>(T value) implements Tree<T> {}

  record Node<T>(Tree<T> left, Tree<T> right) implements Tree<T> {}
}
