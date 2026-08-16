@Test
def testAdd(): Unit \ Assert =
    Assert.assertEq(expected = 5, add(2, 3))

@Test
def testCurriedMultiply(): Unit \ Assert =
    let mult3 = curriedMultiply(3);
    Assert.assertEq(expected = 15, mult3(5))

@Test
def testGenericIdentity(): Unit \ Assert =
    Assert.assertEq(expected = 42, genericIdentity(42))

@Test
def testPipelineDemo(): Unit \ Assert =
    Assert.assertEq(expected = 30, pipelineDemo(10))

@Test
def testRecordDemo(): Unit \ Assert =
    let (x, y, z) = recordDemo();
    Assert.assertEq(expected = 20, x);
    Assert.assertEq(expected = "Flix", y);
    let diff = Float64.abs(z - 3.14);
    Assert.assertTrue(diff < 0.01)

@Test
def testPatternMatchDemo(): Unit \ Assert =
    let result = patternMatchDemo(SimpleResult.Ok(150));
    Assert.assertEq(expected = "Large Ok: 150", result)

@Test
def testLiteralsDemo(): Unit \ Assert =
    Assert.assertEq(expected = 32, literalsDemo())

@Test
def testManyInstantiations(): Unit \ Assert =
    Assert.assertEq(expected = 12, manyInstantiations())

@Test
def testRepeatedStdlibDemo(): Unit \ Assert =
    Assert.assertEq(expected = 19, repeatedStdlibDemo())

@Test
def testLiftedClosuresDemo(): Unit \ Assert =
    Assert.assertEq(expected = 43, liftedClosuresDemo())
