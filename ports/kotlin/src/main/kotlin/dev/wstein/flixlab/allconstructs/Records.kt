package dev.wstein.flixlab.allconstructs

/**
 * Section 10: Records (Construction, Extension, Restriction, Update).
 *
 * Flix's anonymous records support structural extension (`+z`), update (`x = 20 | r`), and
 * restriction (`-y`) on values of literally any matching shape. Kotlin `data class`es are nominal
 * and fixed-shape: "extension" and "restriction" have to become genuinely different types, built
 * by hand from the previous one's fields, rather than a single value growing and shrinking in
 * place. `copy()` covers Flix's update natively.
 *
 * The row-polymorphic `getX` -- "any record with an `x` field, whatever else it carries" -- is
 * approximated with a [HasX] interface that each record explicitly implements, rather than Flix's
 * implicit structural row typing.
 */
data class Base(
    val x: Int,
    val y: String,
)

data class Extended(
    val x: Int,
    val y: String,
    val z: Double,
)

data class Restricted(
    val x: Int,
    val z: Double,
)

data class RecordDemoResult(
    val x: Int,
    val y: String,
    val z: Double,
)

interface HasX<A> {
    val x: A
}

data class PolyRecord(
    override val x: Int,
    val extra: String,
) : HasX<Int>

fun recordDemo(): RecordDemoResult {
    val base = Base(x = 10, y = "Flix")
    val extended = Extended(x = base.x, y = base.y, z = 3.14)
    val updated = extended.copy(x = 20)
    val restricted = Restricted(x = updated.x, z = updated.z)
    return RecordDemoResult(updated.x, updated.y, updated.z)
}

fun <A> getX(r: HasX<A>): A = r.x
