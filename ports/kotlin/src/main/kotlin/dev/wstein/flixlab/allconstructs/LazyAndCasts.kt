package dev.wstein.flixlab.allconstructs

/**
 * Section 15: Lazy Evaluation, Unsafe & Checked/Unchecked Casts. Referencing a `by lazy { }`
 * local property forces it, the same as Flix's separate `lazy`/`force` -- a native mapping, and
 * Kotlin's own `as`/`as?` cover checked/unchecked casts. Flix's `unsafe IO { ... }` block
 * discharges a *static* effect-typing obligation Kotlin function signatures never carry in the
 * first place, so there is nothing to unsafely bypass -- a "no equivalent" gap, not an
 * approximation.
 */
fun lazyAndCastsDemo(): Int {
    val lazyVal by lazy { 10 + 20 }
    val forcedVal: Int = lazyVal
    println("Evaluating (Kotlin has no static effect tracking to bypass)")
    return forcedVal
}
