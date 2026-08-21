package dev.wstein.flixlab.allconstructs

/**
 * Section 16: Specialization Coverage -- repeated stdlib calls. Applies `map`/`filter` at many
 * distinct element types. Kotlin has one `List` type (no separate Vector/List distinction the way
 * Flix and Scala have), so both of Flix's `List.map` and `Vector.map` calls become `List` calls
 * here. Under erasure, `List<A>.map` is one generic function for every `A`; Flix's specializer
 * instead mints one copy of the underlying library def per element type.
 */
fun repeatedStdlibDemo(): Int {
    val a = listOf(1, 2, 3).map { it + 1 }.size
    val b = listOf(1L, 2L).map { it + 1L }.size
    val c = listOf("p", "q").map { "$it!" }.size
    val d = listOf(true, false).map { !it }.size
    val e = listOf(1.0, 2.0).map { it * 2.0 }.size
    val f = listOf(1, 2, 3).filter { it > 1 }.size
    val g = listOf("y", "zz").filter { it.length > 1 }.size
    val h = listOf(1, 2, 3).map { it + 1 }.size
    val i = listOf('a', 'b').map { "$it" }.size
    return a + b + c + d + e + f + g + h + i
}
