package dev.wstein.flixlab.allconstructs

/**
 * Section 2: Type Aliases. `typealias` maps natively onto Flix's own alias syntax -- no
 * approximation needed. Flix's `Pair[a, b] = (a, b)` is skipped here: Kotlin already ships a
 * built-in `kotlin.Pair<A, B>` tuple type, so aliasing it under a new name would only shadow the
 * stdlib one, not add anything.
 */
typealias Predicate<A> = (A) -> Boolean

typealias MathOp = (Int, Int) -> Int

typealias StringMap = Map<String, Int>
