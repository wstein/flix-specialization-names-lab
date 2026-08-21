package dev.wstein.flixlab.allconstructs

/** Section 16: Specialization Coverage -- many instantiations.
  *
  * `describeShape` called at twelve distinct type arguments would mint twelve distinct generated
  * defs in Flix. Under JVM erasure this compiles to one generic method, called twelve times --
  * there is nothing further to specialize.
  */
def describeShape[A](x: A, label: String): String = s"$label=$x"

def manyInstantiations(): Int =
  val parts = List(
    describeShape(1, "i32"),
    describeShape(2.toByte, "i8"),
    describeShape(3.toShort, "i16"),
    describeShape(4L, "i64"),
    describeShape(5.0f, "f32"),
    describeShape(6.0, "f64"),
    describeShape('c', "char"),
    describeShape(true, "bool"),
    describeShape("str", "string"),
    describeShape(Color.Red, "enum"),
    describeShape(Some(9), "option"),
    describeShape(List(1, 2), "list")
  )
  parts.length
