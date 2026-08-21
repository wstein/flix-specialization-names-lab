package dev.wstein.flixlab.allconstructs

import java.math.BigDecimal
import java.math.BigInteger

/**
 * Section 12: Literals, Collections & Monadic For (forM). Kotlin's own primitive types and
 * collection factories are native -- including [Unit], a real singleton value unlike Java's
 * return-type-only `void`. `forM`'s monadic `Option` chaining becomes a nullable `?.let` chain.
 */
fun literalsDemo(): Int {
    val unitVal: Unit = Unit
    val boolVal: Boolean = true
    val charVal: Char = 'F'
    val i8Val: Byte = 8
    val i16Val: Short = 16
    val i32Val: Int = 32
    val i64Val: Long = 64L
    val biVal: BigInteger = BigInteger.valueOf(128)
    val f32Val: Float = 3.14f
    val f64Val: Double = 6.28
    val bdVal: BigDecimal = BigDecimal("12.34")
    val strVal: String = "Hello ${42}"
    val regexVal: Regex = Regex("^[a-zA-Z]+$")
    return i32Val
}

fun collectionsDemo(): List<Int> {
    val list1 = listOf(1, 2, 3)
    val vec1 = listOf(4, 5, 6)
    val set1 = setOf(1, 2, 3)
    val map1 = mapOf(1 to "one", 2 to "two")

    val optResult: Int? = 10.let { x -> 20.let { y -> x + y } }

    return if (optResult == null) list1 else listOf(optResult) + list1
}
