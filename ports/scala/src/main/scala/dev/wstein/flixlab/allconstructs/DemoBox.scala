package dev.wstein.flixlab.allconstructs

/** Section 16: Specialization Coverage -- a polymorphic struct.
  *
  * In Flix, `DemoBox[a, r]` specialized at ten distinct element types mints ten distinct generated
  * classes. On the JVM, generics erase: `DemoBox[A]` compiles to exactly *one* class regardless of
  * how many types it's instantiated at below, and every call reuses that same class at runtime.
  * This is the comparison's headline point -- see docs/ports.md.
  */
final class DemoBox[A](var value: A)

object DemoBox:
  def roundTrip[A](v: A): A =
    val b = new DemoBox(v)
    b.value = v
    b.value

def boxDemo(): String =
  val i = DemoBox.roundTrip(42)
  val s = DemoBox.roundTrip("boxed")
  val b = DemoBox.roundTrip(true)
  val c = DemoBox.roundTrip('x')
  val f = DemoBox.roundTrip(2.5)
  val l = DemoBox.roundTrip(7L)
  val col = DemoBox.roundTrip(Color.Blue)
  val opt = DemoBox.roundTrip(Some(1))
  val lst = DemoBox.roundTrip(List("a", "b"))
  val tup = DemoBox.roundTrip((1, "t"))
  s"$i/$s/$b/$c/$f/$l/$col/$opt/${lst.length}/${tup._1}"
