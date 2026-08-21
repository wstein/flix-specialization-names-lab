package dev.wstein.flixlab.allconstructs

/**
 * Section 16: Specialization Coverage -- many instantiations.
 *
 * `describeShape` called at twelve distinct type arguments would mint twelve distinct generated
 * defs in Flix. Under JVM erasure this compiles to one generic function, called twelve times --
 * there is nothing further to specialize.
 */
fun <A> describeShape(
    x: A,
    label: String,
): String = "$label=$x"

fun manyInstantiations(): Int {
    val parts =
        listOf(
            describeShape(1, "i32"),
            describeShape(2.toByte(), "i8"),
            describeShape(3.toShort(), "i16"),
            describeShape(4L, "i64"),
            describeShape(5.0f, "f32"),
            describeShape(6.0, "f64"),
            describeShape('c', "char"),
            describeShape(true, "bool"),
            describeShape("str", "string"),
            describeShape(Color.RED, "enum"),
            describeShape<Int?>(9, "option"),
            describeShape(listOf(1, 2), "list"),
        )
    return parts.size
}
