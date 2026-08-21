package dev.wstein.flixlab.allconstructs

/** Section 15: Lazy Evaluation, Unsafe & Checked/Unchecked Casts.
  *
  * Referencing a `lazy val` forces it, the same as Flix's separate `lazy`/`force`. Flix's `unsafe
  * IO { ... }` block locally discharges a *static effect-tracking* obligation -- it lets
  * `lazyAndCastsDemo`, whose own signature declares no effect, call `println` (which is `\ IO`)
  * anyway. Scala has no static effect tracking on function signatures at all, so there is nothing
  * to unsafely discharge: `println` is callable from anywhere, unconditionally. This is a "no
  * equivalent" gap, not an approximation -- stated plainly rather than faked with an unrelated
  * `.asInstanceOf`.
  */
def lazyAndCastsDemo(): Int =
  lazy val lazyVal = 10 + 20
  val forcedVal: Int = lazyVal
  println("Evaluating (Scala has no static effect tracking to bypass)")
  forcedVal
