package dev.wstein.flixlab.allconstructs

/**
 * Section 14: Imperative Constructs, Foreach Loops & References in Regions. Flix's `Ref` cell in
 * a region becomes a plain local `var` -- the same block-scoping approximation as section 5's
 * `Person`.
 */
fun imperativeDemo(): Int {
    var total = 0
    val arr = arrayOf(1, 2, 3, 4, 5)

    for (item in arr) {
        total += item
    }

    for (item in listOf(10, 20, 30)) {
        if (item > 15) {
            total += item
        }
    }

    return total
}
