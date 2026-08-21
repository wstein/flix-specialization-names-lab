package dev.wstein.flixlab.allconstructs

import java.util.Comparator
import java.util.function.Supplier

/**
 * Section 16: Specialization Coverage -- anonymous classes. Kotlin would normally prefer
 * SAM-conversion lambdas here; object expressions (`object : Interface { ... }`) are used
 * instead to mirror the literal anonymous-class construct Flix's demo exercises.
 */
fun anonClassDemo(): String {
    val supplier =
        object : Supplier<String> {
            override fun get(): String = "from-anon-class"
        }
    val cmp =
        object : Comparator<String> {
            override fun compare(
                a: String,
                b: String,
            ): Int = a.length - b.length
        }
    val v = supplier.get()
    val c = cmp.compare("aa", "b")
    return "$v/$c"
}
