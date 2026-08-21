package dev.wstein.flixlab.allconstructs

/** Entry point mirroring `AllConstructs.flix`'s `main`, minus the Datalog section (excluded from
  * every port -- see docs/ports.md at the repository root). Sections 12-16 land in the next phase.
  */
object Main:
  def main(args: Array[String]): Unit =
    println("=== AllConstructs (Scala port) ===")

    // Modules & Imports
    val h = HelperModule.helper()
    val mv = HelperModule.moduleValue()
    println(s"Module Helper: $h, Module Value: $mv")

    // Structs & Regions
    val (name, age) = structDemo()
    println(s"Struct Person: $name, Age: $age")

    // Effects & Handlers
    val effRes = runEffectDemo()
    println(s"Effect result: $effRes")

    // Java Interop & Exceptions
    val javaStr = javaInteropDemo()
    val javaExStr = javaExceptionDemo()
    println(s"Java Interop: $javaStr")
    println(s"Java Try/Catch: $javaExStr")

    // Traits & Polymorphism
    val desc1 = describeItem(Color.Green)
    val desc2 = describeItem(123)
    val desc3 = describeItem(Some("nested"): Option[String])
    println(s"Trait describe: $desc1")
    println(s"Trait describe: $desc2")
    println(s"Trait describe: $desc3")

    // Enums & Restrictable Enums
    val shape = Shape.Circle(5.0)
    val area = shapeArea(shape)
    println(s"Restrictable Shape area: $area")

    // Functions, Currying & Pipelines
    val pipeRes = pipelineDemo(10)
    val binPipeRes = binaryPipelineDemo(20, 22)
    val multFn = curriedMultiply(6)
    val multRes = multFn(7)
    val factRes = factorial(5L, 1L)
    println(s"Pipeline: $pipeRes")
    println(s"Binary Pipeline: $binPipeRes")
    println(s"Curried Multiply: $multRes")
    println(s"Factorial(5): $factRes")

    // Records & Pattern Matching
    val (rx, ry, rz) = recordDemo()
    val polyRec = getX(PolyRecord(x = 99, extra = "info"))
    val patMatchRes = patternMatchDemo(SimpleResult.Ok(150))
    val tupleMatchRes = tuplePatternMatch((1, "Status", true))
    println(s"Record: x=$rx, y=$ry, z=$rz")
    println(s"Poly record getX: $polyRec")
    println(s"Pattern match: $patMatchRes")
    println(s"Tuple pattern match: $tupleMatchRes")

    println("=== Sections 1-11 done; 12-16 land in the next phase ===")
