package dev.wstein.flixlab.allconstructs

/**
 * Section 16: Specialization Coverage -- a polymorphic struct.
 *
 * In Flix, `DemoBox[a, r]` specialized at ten distinct element types mints ten distinct generated
 * classes. On the JVM, generics erase: `DemoBox<A>` compiles to exactly *one* class regardless of
 * how many types it's instantiated at below, and every call reuses that same class at runtime.
 * This is the comparison's headline point -- see docs/ports.md.
 */
class DemoBox<A>(
    var value: A,
)

fun <A> demoBoxRoundTrip(v: A): A {
    val b = DemoBox(v)
    b.value = v
    return b.value
}

fun boxDemo(): String {
    val i = demoBoxRoundTrip(42)
    val s = demoBoxRoundTrip("boxed")
    val b = demoBoxRoundTrip(true)
    val c = demoBoxRoundTrip('x')
    val f = demoBoxRoundTrip(2.5)
    val l = demoBoxRoundTrip(7L)
    val col = demoBoxRoundTrip(Color.BLUE)
    val opt: Int? = demoBoxRoundTrip<Int?>(1)
    val lst = demoBoxRoundTrip(listOf("a", "b"))
    val tup = demoBoxRoundTrip(1 to "t")
    return "$i/$s/$b/$c/$f/$l/$col/$opt/${lst.size}/${tup.first}"
}
