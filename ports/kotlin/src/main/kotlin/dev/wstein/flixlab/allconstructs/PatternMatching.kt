package dev.wstein.flixlab.allconstructs

/**
 * Section 11: Pattern Matching & Guards. Native `when`, including smart-cast branches and a
 * built-in [Triple] for the 3-tuple case -- Kotlin's own is at least as expressive as Flix's here.
 * (Kotlin 2.1+ also supports `is X if cond ->` guard syntax directly inside a subject `when`; a
 * nested `when` is used here instead for broad-version compatibility.)
 */
fun patternMatchDemo(x: SimpleResult<Int, String>): String =
    when (x) {
        is SimpleResult.Ok ->
            when {
                x.value > 100 -> "Large Ok: ${x.value}"
                x.value == 0 -> "Zero Ok"
                x.value == 1 -> "One Ok"
                else -> "Normal Ok: ${x.value}"
            }
        is SimpleResult.Err -> "Error: ${x.error}"
    }

fun tuplePatternMatch(p: Triple<Int, String, Boolean>): String {
    val (n, s, b) = p
    return when {
        n == 0 -> "Zero fst"
        b -> "$s true ($n)"
        else -> "False thd"
    }
}
