package dev.wstein.flixlab.allconstructs

/** Section 14: Imperative Constructs, Foreach Loops & References in Regions. Flix's `Ref` cell in a
  * region becomes a plain `var` in a block scope -- the same block-scoping approximation as section
  * 5's `Person`.
  */
def imperativeDemo(): Int =
  var total = 0
  val arr = Array(1, 2, 3, 4, 5)

  for item <- arr do total += item

  for item <- List(10, 20, 30) if item > 15 do total += item

  total
