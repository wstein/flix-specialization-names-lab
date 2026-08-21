package dev.wstein.flixlab.allconstructs

/** Section 16: Specialization Coverage -- repeated stdlib calls. Applies `List.map`/`filter` and
  * `Vector.map` at many distinct element types. Under erasure, `List[A].map` is one generic method
  * for every `A`; Flix's specializer instead mints one copy of the underlying library def per
  * element type.
  */
def repeatedStdlibDemo(): Int =
  val a = List(1, 2, 3).map(_ + 1).length
  val b = List(1L, 2L).map(_ + 1L).length
  val c = List("p", "q").map(x => s"$x!").length
  val d = List(true, false).map(x => !x).length
  val e = List(1.0, 2.0).map(_ * 2.0).length
  val f = List(1, 2, 3).filter(_ > 1).length
  val g = List("y", "zz").filter(_.length > 1).length
  val h = Vector(1, 2, 3).map(_ + 1).length
  val i = Vector('a', 'b').map(x => s"$x").length
  a + b + c + d + e + f + g + h + i
