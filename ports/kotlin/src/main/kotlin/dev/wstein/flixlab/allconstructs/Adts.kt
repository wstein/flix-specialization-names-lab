package dev.wstein.flixlab.allconstructs

/**
 * Section 3: Algebraic Data Types (Enums) & Restrictable Enums.
 *
 * `Color`, `Tree`, and `SimpleResult` map onto Kotlin `enum class`/`sealed interface` + `data
 * class` directly -- structural equality and `toString` come for free on data classes, so there
 * is nothing to "derive" the way Flix spells `with ToString, Eq`.
 *
 * `Shape` approximates Flix's *restrictable* enum, which lets callers be polymorphic over an open
 * subset of cases (via `choose`). Kotlin's `sealed interface` hierarchy is closed once sealed --
 * there is no way to be polymorphic over "some subset of these cases" the way Flix's restrictable
 * enums allow. This is the honest limit of the approximation: same shape, no open-world
 * subsetting.
 */
enum class Color { RED, GREEN, BLUE }

sealed interface Tree<out T> {
    data class Leaf<T>(
        val value: T,
    ) : Tree<T>

    data class Node<T>(
        val left: Tree<T>,
        val right: Tree<T>,
    ) : Tree<T>
}

sealed interface SimpleResult<out T, out E> {
    data class Ok<T, E>(
        val value: T,
    ) : SimpleResult<T, E>

    data class Err<T, E>(
        val error: E,
    ) : SimpleResult<T, E>
}

sealed interface Shape {
    data class Circle(
        val radius: Double,
    ) : Shape

    data class Square(
        val side: Double,
    ) : Shape
}

fun shapeArea(s: Shape): Double =
    when (s) {
        is Shape.Circle -> 3.14159 * s.radius * s.radius
        is Shape.Square -> s.side * s.side
    }
