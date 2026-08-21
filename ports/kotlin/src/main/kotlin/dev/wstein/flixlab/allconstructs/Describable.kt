package dev.wstein.flixlab.allconstructs

/**
 * Section 4: Traits, Instances & Trait Constraints, approximated with dictionary passing: Kotlin
 * has no native typeclass mechanism, so each "instance" is a plain implementation of this
 * interface, passed to [describeItem] explicitly rather than resolved implicitly the way Flix's
 * `with Describable[a]` constraint is.
 *
 * Kotlin has no built-in `Option` type -- idiomatic Kotlin uses nullable types (`T?`) instead, so
 * [NullableDescribable] wraps an inner instance to describe `T?` rather than an `Option<T>`.
 */
interface Describable<T> {
    fun describe(x: T): String

    fun describeTwice(x: T): String = "${describe(x)} & ${describe(x)}"
}

object IntDescribable : Describable<Int> {
    override fun describe(x: Int): String = "Int($x)"
}

object StringDescribable : Describable<String> {
    override fun describe(x: String): String = "String($x)"
}

object ColorDescribable : Describable<Color> {
    override fun describe(x: Color): String =
        when (x) {
            Color.RED -> "Color.Red"
            Color.GREEN -> "Color.Green"
            Color.BLUE -> "Color.Blue"
        }
}

/** Conditional instance: `T?` is describable whenever `T` is, forwarding to the inner value's
 * instance for non-null values.
 */
class NullableDescribable<T : Any>(
    private val inner: Describable<T>,
) : Describable<T?> {
    override fun describe(x: T?): String = if (x == null) "None" else "Some(${inner.describe(x)})"
}

fun <T> describeItem(
    x: T,
    instance: Describable<T>,
): String = instance.describeTwice(x)
