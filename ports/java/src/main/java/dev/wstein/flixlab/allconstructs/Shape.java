package dev.wstein.flixlab.allconstructs;

/**
 * Section 3: approximates Flix's <em>restrictable</em> enum, which lets callers be polymorphic over
 * an open subset of cases (via {@code choose}). A Java {@code sealed} hierarchy is closed once
 * sealed -- there is no way to be polymorphic over "some subset of these cases" the way Flix's
 * restrictable enums allow. This is the honest limit of the approximation: same shape, no
 * open-world subsetting.
 */
public sealed interface Shape permits Shape.Circle, Shape.Square {
  record Circle(double radius) implements Shape {}

  record Square(double side) implements Shape {}

  static double area(Shape s) {
    return switch (s) {
      case Circle c -> 3.14159 * c.radius() * c.radius();
      case Square sq -> sq.side() * sq.side();
    };
  }
}
