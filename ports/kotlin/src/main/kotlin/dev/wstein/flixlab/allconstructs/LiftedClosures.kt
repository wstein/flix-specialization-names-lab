package dev.wstein.flixlab.allconstructs

/**
 * Section 16: Specialization Coverage -- lifted closures.
 *
 * Both closures capture `n`; on the JVM they simply become instances of a synthetic
 * `Function1`-implementing class, same as any other lambda in Kotlin -- there is no separate
 * "specialization" step to observe here the way there is in Flix's lambda-lifting pass.
 */
fun liftedClosures(n: Int): Pair<(Int) -> Int, (Int) -> Int> {
    val f: (Int) -> Int = { x -> x + n }
    val g: (Int) -> Int = { y -> y * n }
    return f to g
}

fun liftedClosuresDemo(): Int {
    val (f, g) = liftedClosures(3)
    return f(10) + g(10)
}
