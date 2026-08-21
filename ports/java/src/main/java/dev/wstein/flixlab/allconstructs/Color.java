package dev.wstein.flixlab.allconstructs;

/**
 * Section 3: Algebraic Data Types (Enums) &amp; Restrictable Enums.
 *
 * <p>Structural equality and {@code toString} come for free on a Java {@code enum} -- nothing to
 * "derive" the way Flix spells {@code with ToString, Eq}.
 */
public enum Color {
  RED,
  GREEN,
  BLUE
}
