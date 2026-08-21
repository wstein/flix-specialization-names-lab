package dev.wstein.flixlab.allconstructs

/** Section 2: Type Aliases. All four map natively onto Scala's own `type` alias syntax -- no
  * approximation needed.
  */
object TypeAliases:
  type Pair[A, B] = (A, B)
  type Predicate[A] = A => Boolean
  type MathOp = (Int, Int) => Int
  type StringMap = Map[String, Int]
