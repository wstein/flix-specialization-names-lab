package dev.wstein.flixlab.allconstructs

/** Section 5: Structs & Companion Module (Internal Mutability with Regions).
  *
  * Flix allocates `Person` in a region `r`, whose lifetime the compiler checks statically. Scala
  * has no equivalent static region checking; the nearest idiomatic approximation is simple block
  * scoping -- the *shape* of "construct, mutate, use, then let go out of scope" survives, but
  * nothing here is checked the way Flix's region typing is. That gap is deliberate, not an
  * oversight.
  */
final class Person(val name: String, var age: Int)

object Person:
  def makeAndAge(name: String, age: Int): (String, Int) =
    val p = Person(name, age)
    p.age += 1
    (p.name, p.age)

def structDemo(): (String, Int) =
  // The block itself stands in for Flix's `region rc { ... }` -- `p` never
  // escapes it, mirroring (without enforcing) the region discipline.
  Person.makeAndAge("Alice", 30)
