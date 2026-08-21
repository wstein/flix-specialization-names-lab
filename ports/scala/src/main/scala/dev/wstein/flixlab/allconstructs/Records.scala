package dev.wstein.flixlab.allconstructs

/** Section 10: Records (Construction, Extension, Restriction, Update).
  *
  * Flix's anonymous records support structural extension (`+z`), update (`x = 20 | r`), and
  * restriction (`-y`) on values of literally any matching shape. Scala's case classes are nominal
  * and fixed-shape: "extension" and "restriction" have to become genuinely different types, built
  * by hand from the previous one's fields, rather than a single value growing and shrinking in
  * place. `copy()` covers Flix's update.
  *
  * The row-polymorphic `getX` -- "any record with an `x` field, whatever else it carries" -- is
  * approximated with a `HasX[A]` interface that each record explicitly implements, rather than
  * Flix's implicit structural row typing.
  */
final case class Base(x: Int, y: String)
final case class Extended(x: Int, y: String, z: Double)
final case class Restricted(x: Int, z: Double)

def recordDemo(): (Int, String, Double) =
  val base = Base(x = 10, y = "Flix")
  val extended = Extended(x = base.x, y = base.y, z = 3.14)
  val updated = extended.copy(x = 20)
  val _restricted = Restricted(x = updated.x, z = updated.z)
  (updated.x, updated.y, updated.z)

trait HasX[A]:
  def x: A

final case class PolyRecord(x: Int, extra: String) extends HasX[Int]

def getX[A](r: HasX[A]): A = r.x
