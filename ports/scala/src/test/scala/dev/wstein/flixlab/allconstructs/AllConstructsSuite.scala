package dev.wstein.flixlab.allconstructs

/** One test per demo function/section, verifying its observable return value -- the same bar
  * `test/TestMain.flix` sets on the Flix side, not a bytecode census (there is nothing to census
  * under JVM erasure) and not output-string parity with Flix's own `.out` snapshot. See
  * docs/ports.md.
  */
class AllConstructsSuite extends munit.FunSuite:

  test("HelperModule") {
    assertEquals(HelperModule.helper(), "HelperModule.helper")
    assertEquals(HelperModule.moduleValue(), 42)
  }

  test("structDemo") {
    assertEquals(structDemo(), ("Alice", 31))
  }

  test("runEffectDemo") {
    assertEquals(runEffectDemo(), 11)
  }

  test("javaInteropDemo") {
    assertEquals(javaInteropDemo(), "Hello from Java Interop!")
  }

  test("javaExceptionDemo") {
    assertEquals(javaExceptionDemo(), "Try/Catch demo")
  }

  test("describeItem") {
    assertEquals(describeItem(Color.Green), "Color.Green & Color.Green")
    assertEquals(describeItem(123), "Int(123) & Int(123)")
    assertEquals(
      describeItem(Some("nested"): Option[String]),
      "Some(String(nested)) & Some(String(nested))"
    )
  }

  test("shapeArea") {
    assertEqualsDouble(shapeArea(Shape.Circle(5.0)), 78.53975, 1e-9)
    assertEqualsDouble(shapeArea(Shape.Square(4.0)), 16.0, 1e-9)
  }

  test("add") {
    assertEquals(add(2, 3), 5)
  }

  test("curriedMultiply") {
    val mult3 = curriedMultiply(3)
    assertEquals(mult3(5), 15)
  }

  test("genericIdentity") {
    assertEquals(genericIdentity(42), 42)
    assertEquals(genericIdentity("x"), "x")
  }

  test("higherOrderDemo") {
    assertEquals(higherOrderDemo(_ + 1, 5), 6)
  }

  test("pipelineDemo") {
    assertEquals(pipelineDemo(10), 30)
  }

  test("binaryPipelineDemo") {
    assertEquals(binaryPipelineDemo(20, 22), 42)
  }

  test("factorial") {
    assertEquals(factorial(5L, 1L), 120L)
  }

  test("recordDemo") {
    val (x, y, z) = recordDemo()
    assertEquals(x, 20)
    assertEquals(y, "Flix")
    assertEqualsDouble(z, 3.14, 1e-9)
  }

  test("getX") {
    assertEquals(getX(PolyRecord(x = 99, extra = "info")), 99)
  }

  test("patternMatchDemo") {
    assertEquals(patternMatchDemo(SimpleResult.Ok(150)), "Large Ok: 150")
    assertEquals(patternMatchDemo(SimpleResult.Ok(0)), "Zero Ok")
    assertEquals(patternMatchDemo(SimpleResult.Ok(1)), "One Ok")
    assertEquals(patternMatchDemo(SimpleResult.Ok(5)), "Normal Ok: 5")
    assertEquals(patternMatchDemo(SimpleResult.Err("boom")), "Error: boom")
  }

  test("tuplePatternMatch") {
    assertEquals(tuplePatternMatch((0, "x", true)), "Zero fst")
    assertEquals(tuplePatternMatch((1, "Status", true)), "Status true (1)")
    assertEquals(tuplePatternMatch((2, "x", false)), "False thd")
  }

  test("literalsDemo") {
    assertEquals(literalsDemo(), 32)
  }

  test("collectionsDemo") {
    assertEquals(collectionsDemo(), List(30, 1, 2, 3))
  }

  test("imperativeDemo") {
    assertEquals(imperativeDemo(), 65)
  }

  test("lazyAndCastsDemo") {
    assertEquals(lazyAndCastsDemo(), 30)
  }

  test("parYieldDemo") {
    assertEquals(parYieldDemo(), (3, "hello world"))
  }

  test("concurrencyDemo") {
    assertEquals(concurrencyDemo(), 42)
  }

  test("selectDemo") {
    assertEquals(selectDemo(), "Hello Select")
  }

  test("boxDemo") {
    assertEquals(boxDemo(), "42/boxed/true/x/2.5/7/Blue/Some(1)/2/1")
  }

  test("anonClassDemo") {
    assertEquals(anonClassDemo(), "from-anon-class/1")
  }

  test("manyInstantiations") {
    assertEquals(manyInstantiations(), 12)
  }

  test("liftedClosuresDemo") {
    assertEquals(liftedClosuresDemo(), 43)
  }

  test("repeatedStdlibDemo") {
    assertEquals(repeatedStdlibDemo(), 19)
  }
