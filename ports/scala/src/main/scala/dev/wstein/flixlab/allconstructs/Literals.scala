package dev.wstein.flixlab.allconstructs

import scala.util.matching.Regex

/** Section 12: Literals, Collections & Monadic For (forM). All native -- Scala's own primitive
  * types, collection literals, and for-comprehension over `Option` cover Flix's `forM` directly.
  */
def literalsDemo(): Int =
  val _unitVal: Unit = ()
  val _boolVal: Boolean = true
  val _charVal: Char = 'F'
  val _i8Val: Byte = 8.toByte
  val _i16Val: Short = 16.toShort
  val i32Val: Int = 32
  val _i64Val: Long = 64L
  val _biVal: BigInt = BigInt(128)
  val _f32Val: Float = 3.14f
  val _f64Val: Double = 6.28
  val _bdVal: BigDecimal = BigDecimal(12.34)
  val _strVal: String = s"Hello ${42}"
  val _regexVal: Regex = "^[a-zA-Z]+$".r
  i32Val

def collectionsDemo(): List[Int] =
  val list1 = List(1, 2, 3)
  val _vec1 = Vector(4, 5, 6)
  val _set1 = Set(1, 2, 3)
  val _map1 = Map(1 -> "one", 2 -> "two")

  val optResult: Option[Int] =
    for
      x <- Some(10)
      y <- Some(20)
    yield x + y

  optResult match
    case None    => list1
    case Some(v) => v :: list1
