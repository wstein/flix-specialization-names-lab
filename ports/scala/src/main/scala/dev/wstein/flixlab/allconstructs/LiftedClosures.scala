package dev.wstein.flixlab.allconstructs

/** Section 16: Specialization Coverage -- lifted closures.
  *
  * Both closures capture `n`; on the JVM they simply become instances of synthetic `Function1`
  * implementations, same as any other lambda in Scala -- there is no separate "specialization" step
  * to observe here the way there is in Flix's lambda-lifting pass.
  */
def liftedClosures(n: Int): (Int => Int, Int => Int) =
  val f = (x: Int) => x + n
  val g = (y: Int) => y * n
  (f, g)

def liftedClosuresDemo(): Int =
  val (f, g) = liftedClosures(3)
  f(10) + g(10)
