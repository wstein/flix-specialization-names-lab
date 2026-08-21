package dev.wstein.flixlab.allconstructs;

import java.util.Optional;

/**
 * Entry point mirroring {@code AllConstructs.flix}'s {@code main}, minus the Datalog section
 * (excluded from every port -- see {@code docs/ports.md} at the repository root).
 */
public final class Main {
  private Main() {}

  public static void main(String[] args) {
    System.out.println("=== AllConstructs (Java port) ===");

    // Modules & Imports
    String h = HelperModule.helper();
    int mv = HelperModule.moduleValue();
    System.out.println("Module Helper: " + h + ", Module Value: " + mv);

    // Structs & Regions
    Person.NameAge nameAge = Person.structDemo();
    System.out.println("Struct Person: " + nameAge.name() + ", Age: " + nameAge.age());

    // Effects & Handlers
    int effRes = Effects.runEffectDemo();
    System.out.println("Effect result: " + effRes);

    // Java Interop & Exceptions
    String javaStr = JavaInterop.javaInteropDemo();
    String javaExStr = JavaInterop.javaExceptionDemo();
    System.out.println("Java Interop: " + javaStr);
    System.out.println("Java Try/Catch: " + javaExStr);

    // Traits & Polymorphism
    String desc1 = Describable.describeItem(Color.GREEN, Describable.forColor());
    String desc2 = Describable.describeItem(123, Describable.forInt());
    String desc3 =
        Describable.describeItem(
            Optional.of("nested"), Describable.forOptional(Describable.forString()));
    System.out.println("Trait describe: " + desc1);
    System.out.println("Trait describe: " + desc2);
    System.out.println("Trait describe: " + desc3);

    // Enums & Restrictable Enums
    Shape shape = new Shape.Circle(5.0);
    double area = Shape.area(shape);
    System.out.println("Restrictable Shape area: " + area);

    // Functions, Currying & Pipelines
    int pipeRes = Functions.pipelineDemo(10);
    int binPipeRes = Functions.binaryPipelineDemo(20, 22);
    var multFn = Functions.curriedMultiply(6);
    int multRes = multFn.applyAsInt(7);
    long factRes = Functions.factorial(5L, 1L);
    System.out.println("Pipeline: " + pipeRes);
    System.out.println("Binary Pipeline: " + binPipeRes);
    System.out.println("Curried Multiply: " + multRes);
    System.out.println("Factorial(5): " + factRes);

    // Records & Pattern Matching
    Records.RecordDemoResult record = Records.recordDemo();
    int polyRec = Records.getX(new Records.PolyRecord(99, "info"));
    String patMatchRes =
        PatternMatchingOps.patternMatchDemo(new SimpleResult.Ok<Integer, String>(150));
    String tupleMatchRes =
        PatternMatchingOps.tuplePatternMatch(new PatternMatchingOps.Triple(1, "Status", true));
    System.out.println("Record: x=" + record.x() + ", y=" + record.y() + ", z=" + record.z());
    System.out.println("Poly record getX: " + polyRec);
    System.out.println("Pattern match: " + patMatchRes);
    System.out.println("Tuple pattern match: " + tupleMatchRes);

    // Literals & Collections & Comprehensions
    int lit = Literals.literalsDemo();
    var colList = Literals.collectionsDemo();
    System.out.println("Collections result: " + colList);

    // Imperative & References
    int impRes = Imperative.imperativeDemo();
    System.out.println("Imperative total: " + impRes);

    // Lazy & Casts
    int lazyRes = LazyAndCasts.lazyAndCastsDemo();
    System.out.println("Lazy/Casts result: " + lazyRes);

    // Parallelism & Concurrency & Select
    var parYieldRes = Concurrency.parYieldDemo();
    System.out.println("Par yield: (" + parYieldRes.x() + ", " + parYieldRes.y() + ")");
    int chanVal = Concurrency.concurrencyDemo();
    System.out.println("Channel received: " + chanVal);
    String selectVal = Concurrency.selectDemo();
    System.out.println("Select received: " + selectVal);

    // Specialization Coverage
    System.out.println("DemoBox (polymorphic struct): " + DemoBox.boxDemo());
    System.out.println("Anon class: " + AnonClass.anonClassDemo());
    System.out.println("Instantiations: " + ManyInstantiations.manyInstantiations());
    System.out.println("Lifted closures: " + LiftedClosures.liftedClosuresDemo());
    System.out.println("Repeated stdlib: " + RepeatedStdlib.repeatedStdlibDemo());

    System.out.println("=== Done ===");
  }
}
