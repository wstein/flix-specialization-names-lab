package dev.wstein.flixlab.allconstructs

/**
 * Section 9: Functions, Currying, Pipelines, Lambdas & Tail Recursion. All native -- `.let` is
 * the direct counterpart of Flix's `|>`, and `tailrec fun` gives compiler-*verified* tail
 * recursion (Kotlin errors, rather than silently failing to optimize, if a `tailrec` function
 * isn't actually tail-recursive).
 */
fun add(
    x: Int,
    y: Int,
): Int = x + y

fun curriedMultiply(x: Int): (Int) -> Int = { y -> x * y }

fun <A> genericIdentity(x: A): A = x

fun higherOrderDemo(
    f: (Int) -> Int,
    x: Int,
): Int = f(x)

fun pipelineDemo(x: Int): Int = x.let { it + 5 }.let { it * 2 }

fun binaryPipelineDemo(
    a: Int,
    b: Int,
): Int = add(a, b)

tailrec fun factorial(
    n: Long,
    acc: Long,
): Long = if (n <= 1L) acc else factorial(n - 1L, n * acc)
