package dev.wstein.flixlab.allconstructs

/**
 * Entry point mirroring `AllConstructs.flix`'s `main`, minus the Datalog section (excluded from
 * every port -- see docs/ports.md at the repository root).
 */
fun main() {
    println("=== AllConstructs (Kotlin port) ===")

    // Modules & Imports
    val h = HelperModule.helper()
    val mv = HelperModule.moduleValue()
    println("Module Helper: $h, Module Value: $mv")

    // Structs & Regions
    val (name, age) = structDemo()
    println("Struct Person: $name, Age: $age")

    // Effects & Handlers
    val effRes = runEffectDemo()
    println("Effect result: $effRes")

    // Java Interop & Exceptions
    val javaStr = javaInteropDemo()
    val javaExStr = javaExceptionDemo()
    println("Java Interop: $javaStr")
    println("Java Try/Catch: $javaExStr")

    // Traits & Polymorphism
    val desc1 = describeItem(Color.GREEN, ColorDescribable)
    val desc2 = describeItem(123, IntDescribable)
    val desc3 = describeItem<String?>("nested", NullableDescribable(StringDescribable))
    println("Trait describe: $desc1")
    println("Trait describe: $desc2")
    println("Trait describe: $desc3")

    // Enums & Restrictable Enums
    val shape = Shape.Circle(5.0)
    val area = shapeArea(shape)
    println("Restrictable Shape area: $area")

    // Functions, Currying & Pipelines
    val pipeRes = pipelineDemo(10)
    val binPipeRes = binaryPipelineDemo(20, 22)
    val multFn = curriedMultiply(6)
    val multRes = multFn(7)
    val factRes = factorial(5L, 1L)
    println("Pipeline: $pipeRes")
    println("Binary Pipeline: $binPipeRes")
    println("Curried Multiply: $multRes")
    println("Factorial(5): $factRes")

    // Records & Pattern Matching
    val record = recordDemo()
    val polyRec = getX(PolyRecord(x = 99, extra = "info"))
    val patMatchRes = patternMatchDemo(SimpleResult.Ok(150))
    val tupleMatchRes = tuplePatternMatch(Triple(1, "Status", true))
    println("Record: x=${record.x}, y=${record.y}, z=${record.z}")
    println("Poly record getX: $polyRec")
    println("Pattern match: $patMatchRes")
    println("Tuple pattern match: $tupleMatchRes")

    // Literals & Collections & Comprehensions
    val lit = literalsDemo()
    val colList = collectionsDemo()
    println("Collections result: $colList")

    // Imperative & References
    val impRes = imperativeDemo()
    println("Imperative total: $impRes")

    // Lazy & Casts
    val lazyRes = lazyAndCastsDemo()
    println("Lazy/Casts result: $lazyRes")

    // Parallelism & Concurrency & Select
    val (py1, py2) = parYieldDemo()
    println("Par yield: ($py1, $py2)")
    val chanVal = concurrencyDemo()
    println("Channel received: $chanVal")
    val selectVal = selectDemo()
    println("Select received: $selectVal")

    // Specialization Coverage
    println("DemoBox (polymorphic struct): ${boxDemo()}")
    println("Anon class: ${anonClassDemo()}")
    println("Instantiations: ${manyInstantiations()}")
    println("Lifted closures: ${liftedClosuresDemo()}")
    println("Repeated stdlib: ${repeatedStdlibDemo()}")

    println("=== Done ===")
}
