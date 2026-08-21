package dev.wstein.flixlab.allconstructs

import java.io.Closeable

/**
 * Section 5: Structs & Companion Module (Internal Mutability with Regions).
 *
 * Flix allocates `Person` in a region `r`, whose lifetime the compiler checks statically. Kotlin
 * has no equivalent static region checking; the nearest idiomatic approximation is [Closeable] +
 * `use { }` -- the *shape* of "construct, mutate, use, then let go out of scope" survives, but
 * nothing here is checked the way Flix's region typing is. That gap is deliberate, not an
 * oversight.
 */
class Person(
    val name: String,
    var age: Int,
) : Closeable {
    override fun close() {
        // No real resource to release -- this exists only so Person can be used with
        // Closeable.use { }, approximating Flix's region-scoped lifetime.
    }

    companion object {
        fun makeAndAge(
            name: String,
            age: Int,
        ): Pair<String, Int> =
            Person(name, age).use { p ->
                p.age += 1
                p.name to p.age
            }
    }
}

fun structDemo(): Pair<String, Int> = Person.makeAndAge("Alice", 30)
