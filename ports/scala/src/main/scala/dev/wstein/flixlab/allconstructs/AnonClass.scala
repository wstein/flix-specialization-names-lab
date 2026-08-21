package dev.wstein.flixlab.allconstructs

import java.util.Comparator
import java.util.function.Supplier

/** Section 16: Specialization Coverage -- anonymous classes. Scala instantiates Java functional
  * interfaces with the same anonymous-class syntax Flix uses -- a direct, unapproximated mapping.
  */
def anonClassDemo(): String =
  val supplier = new Supplier[String]:
    def get(): String = "from-anon-class"
  val cmp = new Comparator[String]:
    def compare(a: String, b: String): Int = a.length - b.length
  val v = supplier.get()
  val c = cmp.compare("aa", "b")
  s"$v/$c"
