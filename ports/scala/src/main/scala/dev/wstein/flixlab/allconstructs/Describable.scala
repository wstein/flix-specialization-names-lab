package dev.wstein.flixlab.allconstructs

/** Section 4: Traits, Instances & Trait Constraints.
  *
  * A typeclass for types that can render themselves as a human-readable description. Scala 3's
  * `trait` + `given` instances are a native typeclass mechanism -- no dictionary-passing
  * approximation is needed here the way Java and Kotlin require.
  */
trait Describable[T]:
  def describe(x: T): String
  def describeTwice(x: T): String = s"${describe(x)} & ${describe(x)}"

given Describable[Int] with
  def describe(x: Int): String = s"Int($x)"

given Describable[String] with
  def describe(x: String): String = s"String($x)"

given Describable[Color] with
  def describe(x: Color): String = x match
    case Color.Red   => "Color.Red"
    case Color.Green => "Color.Green"
    case Color.Blue  => "Color.Blue"

/** Conditional instance: `Option[T]` is `Describable` whenever `T` is, forwarding to the inner
  * value's instance for `Some`.
  */
given [T](using inner: Describable[T]): Describable[Option[T]] with
  def describe(x: Option[T]): String = x match
    case None    => "None"
    case Some(v) => s"Some(${inner.describe(v)})"

/** Describes `x` twice, for any type with a `Describable` instance. */
def describeItem[T](x: T)(using d: Describable[T]): String = d.describeTwice(x)
