package dev.wstein.flixlab.allconstructs

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * One test per demo function/section, verifying its observable return value -- the same bar
 * `test/TestMain.flix` sets on the Flix side, not a bytecode census (there is nothing to census
 * under JVM erasure) and not output-string parity with Flix's own `.out` snapshot. See
 * docs/ports.md.
 *
 * Test names are backticked descriptions rather than the demo function's own name: a member
 * function shadows a top-level one of the same name, so `fun structDemo() { structDemo() }` would
 * call itself recursively instead of the top-level function under test.
 */
class AllConstructsTest {
    @Test
    fun `helper module returns its greeting and constant`() {
        assertEquals("HelperModule.helper", HelperModule.helper())
        assertEquals(42, HelperModule.moduleValue())
    }

    @Test
    fun `struct demo ages the person by one year`() {
        assertEquals("Alice" to 31, structDemo())
    }

    @Test
    fun `effect demo increments the seeded state`() {
        assertEquals(11, runEffectDemo())
    }

    @Test
    fun `java interop demo builds a string via StringBuilder`() {
        assertEquals("Hello from Java Interop!", javaInteropDemo())
    }

    @Test
    fun `java exception demo succeeds when nothing throws`() {
        assertEquals("Try/Catch demo", javaExceptionDemo())
    }

    @Test
    fun `describeItem dispatches to each dictionary instance`() {
        assertEquals("Color.Green & Color.Green", describeItem(Color.GREEN, ColorDescribable))
        assertEquals("Int(123) & Int(123)", describeItem(123, IntDescribable))
        assertEquals(
            "Some(String(nested)) & Some(String(nested))",
            describeItem<String?>("nested", NullableDescribable(StringDescribable)),
        )
    }

    @Test
    fun `shape area computes circle and square area`() {
        assertEquals(78.53975, shapeArea(Shape.Circle(5.0)), 1e-9)
        assertEquals(16.0, shapeArea(Shape.Square(4.0)), 1e-9)
    }

    @Test
    fun `add sums two integers`() {
        assertEquals(5, add(2, 3))
    }

    @Test
    fun `curried multiply returns a partially applied function`() {
        val mult3 = curriedMultiply(3)
        assertEquals(15, mult3(5))
    }

    @Test
    fun `generic identity returns its argument unchanged`() {
        assertEquals(42, genericIdentity(42))
        assertEquals("x", genericIdentity("x"))
    }

    @Test
    fun `higher order demo applies the given function`() {
        assertEquals(6, higherOrderDemo({ it + 1 }, 5))
    }

    @Test
    fun `pipeline demo chains two lambdas via let`() {
        assertEquals(30, pipelineDemo(10))
    }

    @Test
    fun `binary pipeline demo adds a pair`() {
        assertEquals(42, binaryPipelineDemo(20, 22))
    }

    @Test
    fun `factorial is tail recursive`() {
        assertEquals(120L, factorial(5L, 1L))
    }

    @Test
    fun `record demo extends updates and restricts a record`() {
        val result = recordDemo()
        assertEquals(20, result.x)
        assertEquals("Flix", result.y)
        assertEquals(3.14, result.z, 1e-9)
    }

    @Test
    fun `getX projects the x field via HasX`() {
        assertEquals(99, getX(PolyRecord(x = 99, extra = "info")))
    }

    @Test
    fun `pattern match demo matches guards and literals`() {
        assertEquals("Large Ok: 150", patternMatchDemo(SimpleResult.Ok(150)))
        assertEquals("Zero Ok", patternMatchDemo(SimpleResult.Ok(0)))
        assertEquals("One Ok", patternMatchDemo(SimpleResult.Ok(1)))
        assertEquals("Normal Ok: 5", patternMatchDemo(SimpleResult.Ok(5)))
        assertEquals("Error: boom", patternMatchDemo(SimpleResult.Err("boom")))
    }

    @Test
    fun `tuple pattern match destructures a Triple`() {
        assertEquals("Zero fst", tuplePatternMatch(Triple(0, "x", true)))
        assertEquals("Status true (1)", tuplePatternMatch(Triple(1, "Status", true)))
        assertEquals("False thd", tuplePatternMatch(Triple(2, "x", false)))
    }

    @Test
    fun `literals demo returns the Int32 literal`() {
        assertEquals(32, literalsDemo())
    }

    @Test
    fun `collections demo conses the forM result onto the list`() {
        assertEquals(listOf(30, 1, 2, 3), collectionsDemo())
    }

    @Test
    fun `imperative demo sums the array and filtered list`() {
        assertEquals(65, imperativeDemo())
    }

    @Test
    fun `lazy and casts demo forces the lazy value`() {
        assertEquals(30, lazyAndCastsDemo())
    }

    @Test
    fun `par yield demo evaluates two expressions in parallel`() {
        assertEquals(3 to "hello world", parYieldDemo())
    }

    @Test
    fun `concurrency demo receives from a channel`() {
        assertEquals(42, concurrencyDemo())
    }

    @Test
    fun `select demo receives from whichever channel is ready`() {
        assertEquals("Hello Select", selectDemo())
    }

    @Test
    fun `box demo round-trips ten distinct element types`() {
        assertEquals("42/boxed/true/x/2.5/7/BLUE/1/2/1", boxDemo())
    }

    @Test
    fun `anon class demo instantiates two functional interfaces`() {
        assertEquals("from-anon-class/1", anonClassDemo())
    }

    @Test
    fun `many instantiations calls describeShape at twelve types`() {
        assertEquals(12, manyInstantiations())
    }

    @Test
    fun `lifted closures demo combines both returned closures`() {
        assertEquals(43, liftedClosuresDemo())
    }

    @Test
    fun `repeated stdlib demo applies map and filter at many types`() {
        assertEquals(19, repeatedStdlibDemo())
    }
}
