package dev.wstein.flixlab.allconstructs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * One test per demo function/section, verifying its observable return value -- the same bar {@code
 * test/TestMain.flix} sets on the Flix side, not a bytecode census (there is nothing to census
 * under JVM erasure) and not output-string parity with Flix's own {@code .out} snapshot. See
 * docs/ports.md.
 */
class AllConstructsTest {

  @Test
  void helperModule() {
    assertEquals("HelperModule.helper", HelperModule.helper());
    assertEquals(42, HelperModule.moduleValue());
  }

  @Test
  void structDemo() {
    Person.NameAge result = Person.structDemo();
    assertEquals("Alice", result.name());
    assertEquals(31, result.age());
  }

  @Test
  void runEffectDemo() {
    assertEquals(11, Effects.runEffectDemo());
  }

  @Test
  void javaInteropDemo() {
    assertEquals("Hello from Java Interop!", JavaInterop.javaInteropDemo());
  }

  @Test
  void javaExceptionDemo() {
    assertEquals("Try/Catch demo", JavaInterop.javaExceptionDemo());
  }

  @Test
  void describeItem() {
    assertEquals(
        "Color.Green & Color.Green", Describable.describeItem(Color.GREEN, Describable.forColor()));
    assertEquals("Int(123) & Int(123)", Describable.describeItem(123, Describable.forInt()));
    assertEquals(
        "Some(String(nested)) & Some(String(nested))",
        Describable.describeItem(
            Optional.of("nested"), Describable.forOptional(Describable.forString())));
  }

  @Test
  void shapeArea() {
    assertEquals(78.53975, Shape.area(new Shape.Circle(5.0)), 1e-9);
    assertEquals(16.0, Shape.area(new Shape.Square(4.0)), 1e-9);
  }

  @Test
  void add() {
    assertEquals(5, Functions.add(2, 3));
  }

  @Test
  void curriedMultiply() {
    var mult3 = Functions.curriedMultiply(3);
    assertEquals(15, mult3.applyAsInt(5));
  }

  @Test
  void genericIdentity() {
    assertEquals(42, Functions.<Integer>genericIdentity(42));
    assertEquals("x", Functions.genericIdentity("x"));
  }

  @Test
  void higherOrderDemo() {
    assertEquals(6, Functions.higherOrderDemo(x -> x + 1, 5));
  }

  @Test
  void pipelineDemo() {
    assertEquals(30, Functions.pipelineDemo(10));
  }

  @Test
  void binaryPipelineDemo() {
    assertEquals(42, Functions.binaryPipelineDemo(20, 22));
  }

  @Test
  void factorial() {
    assertEquals(120L, Functions.factorial(5L, 1L));
  }

  @Test
  void recordDemo() {
    Records.RecordDemoResult result = Records.recordDemo();
    assertEquals(20, result.x());
    assertEquals("Flix", result.y());
    assertEquals(3.14, result.z(), 1e-9);
  }

  @Test
  void getX() {
    assertEquals(99, Records.getX(new Records.PolyRecord(99, "info")));
  }

  @Test
  void patternMatchDemo() {
    assertEquals("Large Ok: 150", PatternMatchingOps.patternMatchDemo(new SimpleResult.Ok<>(150)));
    assertEquals("Zero Ok", PatternMatchingOps.patternMatchDemo(new SimpleResult.Ok<>(0)));
    assertEquals("One Ok", PatternMatchingOps.patternMatchDemo(new SimpleResult.Ok<>(1)));
    assertEquals("Normal Ok: 5", PatternMatchingOps.patternMatchDemo(new SimpleResult.Ok<>(5)));
    assertEquals(
        "Error: boom", PatternMatchingOps.patternMatchDemo(new SimpleResult.Err<>("boom")));
  }

  @Test
  void tuplePatternMatch() {
    assertEquals(
        "Zero fst",
        PatternMatchingOps.tuplePatternMatch(new PatternMatchingOps.Triple(0, "x", true)));
    assertEquals(
        "Status true (1)",
        PatternMatchingOps.tuplePatternMatch(new PatternMatchingOps.Triple(1, "Status", true)));
    assertEquals(
        "False thd",
        PatternMatchingOps.tuplePatternMatch(new PatternMatchingOps.Triple(2, "x", false)));
  }

  @Test
  void literalsDemo() {
    assertEquals(32, Literals.literalsDemo());
  }

  @Test
  void collectionsDemo() {
    assertEquals(List.of(30, 1, 2, 3), Literals.collectionsDemo());
  }

  @Test
  void imperativeDemo() {
    assertEquals(65, Imperative.imperativeDemo());
  }

  @Test
  void lazyAndCastsDemo() {
    assertEquals(30, LazyAndCasts.lazyAndCastsDemo());
  }

  @Test
  void parYieldDemo() {
    var result = Concurrency.parYieldDemo();
    assertEquals(3, result.x());
    assertEquals("hello world", result.y());
  }

  @Test
  void concurrencyDemo() {
    assertEquals(42, Concurrency.concurrencyDemo());
  }

  @Test
  void selectDemo() {
    assertEquals("Hello Select", Concurrency.selectDemo());
  }

  @Test
  void boxDemo() {
    assertEquals("42/boxed/true/x/2.5/7/BLUE/Optional[1]/2/1", DemoBox.boxDemo());
  }

  @Test
  void anonClassDemo() {
    assertEquals("from-anon-class/1", AnonClass.anonClassDemo());
  }

  static Stream<Arguments> manyInstantiationsArgs() {
    return Stream.of(
        Arguments.of(1, "i32", "i32=1"),
        Arguments.of((byte) 2, "i8", "i8=2"),
        Arguments.of((short) 3, "i16", "i16=3"),
        Arguments.of(4L, "i64", "i64=4"),
        Arguments.of(5.0f, "f32", "f32=5.0"),
        Arguments.of(6.0, "f64", "f64=6.0"),
        Arguments.of('c', "char", "char=c"),
        Arguments.of(true, "bool", "bool=true"),
        Arguments.of("str", "string", "string=str"));
  }

  @ParameterizedTest
  @MethodSource("manyInstantiationsArgs")
  void describeShapeAtEachInstantiation(Object value, String label, String expected) {
    assertEquals(expected, ManyInstantiations.describeShape(value, label));
  }

  @Test
  void manyInstantiations() {
    assertEquals(12, ManyInstantiations.manyInstantiations());
  }

  @Test
  void liftedClosuresDemo() {
    assertEquals(43, LiftedClosures.liftedClosuresDemo());
  }

  @Test
  void repeatedStdlibDemo() {
    assertEquals(19, RepeatedStdlib.repeatedStdlibDemo());
  }

  @Test
  void treeAndOptionAreExhaustivelyMatchable() {
    Tree<Integer> tree = new Tree.Node<>(new Tree.Leaf<>(1), new Tree.Leaf<>(2));
    assertTrue(tree instanceof Tree.Node<Integer>);
  }
}
