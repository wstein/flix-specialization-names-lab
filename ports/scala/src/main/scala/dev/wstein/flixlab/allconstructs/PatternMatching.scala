package dev.wstein.flixlab.allconstructs

/** Section 11: Pattern Matching & Guards. Native `match`, including guards and tuple patterns --
  * Scala's own is at least as expressive as Flix's here, no approximation needed.
  */
def patternMatchDemo(x: SimpleResult[Int, String]): String = x match
  case SimpleResult.Ok(v) if v > 100 => s"Large Ok: $v"
  case SimpleResult.Ok(0)            => "Zero Ok"
  case SimpleResult.Ok(1)            => "One Ok"
  case SimpleResult.Ok(v)            => s"Normal Ok: $v"
  case SimpleResult.Err(msg)         => s"Error: $msg"

def tuplePatternMatch(p: (Int, String, Boolean)): String = p match
  case (0, _, _)      => "Zero fst"
  case (n, str, true) => s"$str true ($n)"
  case (_, _, false)  => "False thd"
