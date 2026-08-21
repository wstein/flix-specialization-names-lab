package dev.wstein.flixlab.allconstructs

/** Section 3: Algebraic Data Types (Enums) & Restrictable Enums.
  *
  * `Color`, `Tree`, and `SimpleResult` map onto Scala 3 `enum` directly -- structural equality and
  * `toString` come for free on enum cases, so there is nothing to "derive" the way Flix spells
  * `with ToString, Eq`.
  *
  * `Shape` approximates Flix's *restrictable* enum, which lets callers be polymorphic over an open
  * subset of cases (via `choose`). Scala's `sealed trait` hierarchy is closed once sealed -- there
  * is no way to be polymorphic over "some subset of these cases" the way Flix's restrictable enums
  * allow. This is the honest limit of the approximation: same shape, no open-world subsetting.
  */
enum Color:
  case Red, Green, Blue

enum Tree[+T]:
  case Leaf(value: T)
  case Node(left: Tree[T], right: Tree[T])

enum SimpleResult[+T, +E]:
  case Ok(value: T)
  case Err(error: E)

sealed trait Shape
object Shape:
  final case class Circle(radius: Double) extends Shape
  final case class Square(side: Double) extends Shape

def shapeArea(s: Shape): Double = s match
  case Shape.Circle(r) => 3.14159 * r * r
  case Shape.Square(w) => w * w
