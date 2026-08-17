// ============================================================================
// Flix Comprehensive Syntax & Language Constructs Specification Demo
// ============================================================================

import java.lang.Math
import java.lang.StringBuilder
import java.lang.Exception
import java.util.Comparator
import java.util.function.Supplier

// ----------------------------------------------------------------------------
// 1. Modules, Visibility & Imports
// ----------------------------------------------------------------------------

/// A nested module whose members are used from `main` via `use`, to exercise
/// namespaced access and qualified imports.
mod HelperModule {
    /// Returns a fixed greeting string identifying this module.
    pub def helper(): String = "HelperModule.helper"

    /// Returns a fixed `Int32` constant owned by this module.
    pub def moduleValue(): Int32 = 42
}

// ----------------------------------------------------------------------------
// 2. Type Aliases
// ----------------------------------------------------------------------------

/// A generic 2-tuple, aliased under a domain-specific name.
type alias Pair[a, b] = (a, b)

/// A function from `a` to `Bool`, i.e. a predicate over `a`.
type alias Predicate[a] = a -> Bool

/// A binary `Int32` operator, e.g. `+` or `*`.
type alias MathOp = (Int32, Int32) -> Int32

/// A map keyed by `String` with `Int32` values.
type alias StringMap = Map[String, Int32]

// ----------------------------------------------------------------------------
// 3. Algebraic Data Types (Enums) & Restrictable Enums
// ----------------------------------------------------------------------------

/// A simple enum with derived `ToString` and `Eq` instances.
enum Color with ToString, Eq {
    case Red,
    case Green,
    case Blue
}

/// A parametric, recursive algebraic data type representing a binary tree
/// whose leaves carry a value of type `t`.
enum Tree[t] with ToString {
    case Leaf(t),
    case Node(Tree[t], Tree[t])
}

/// A custom two-case result type, distinct from the standard library's
/// `Result`, used to demonstrate a hand-rolled `Ok`/`Err` enum.
enum SimpleResult[t, e] with ToString {
    case Ok(t),
    case Err(e)
}

/// A restrictable enum (open variant) over shapes. Unlike a regular enum,
/// callers can be polymorphic over which subset of cases they accept.
restrictable enum Shape[s] {
    case Circle(Float64),
    case Square(Float64)
}

/// Computes the area of `s`, dispatching on its `Circle` or `Square` case via
/// `choose`.
def shapeArea(s: Shape[s]): Float64 = choose s {
    case Shape.Circle(r) => 3.14159 * r * r
    case Shape.Square(w) => w * w
}

// ----------------------------------------------------------------------------
// 4. Traits, Instances & Trait Constraints
// ----------------------------------------------------------------------------

/// A trait for types that can render themselves as a human-readable
/// description.
pub trait Describable[t] {
    /// Describes `x` as a string.
    pub def describe(x: t): String

    /// Describes `x` twice, joined by `&`. Has a default implementation in
    /// terms of `describe`, so instances only need to implement `describe`.
    pub def describeTwice(x: t): String = "${Describable.describe(x)} & ${Describable.describe(x)}"
}

instance Describable[Int32] {
    pub def describe(x: Int32): String = "Int32(${x})"
}

instance Describable[String] {
    pub def describe(x: String): String = "String(${x})"
}

instance Describable[Color] {
    pub def describe(x: Color): String = match x {
        case Color.Red   => "Color.Red"
        case Color.Green => "Color.Green"
        case Color.Blue  => "Color.Blue"
    }
}

/// A conditional instance: `Option[t]` is `Describable` whenever `t` is,
/// forwarding to the inner value's instance for `Some`.
instance Describable[Option[t]] with Describable[t] {
    pub def describe(x: Option[t]): String = match x {
        case None    => "None"
        case Some(v) => "Some(${Describable.describe(v)})"
    }
}

/// Describes `x` twice, for any type with a `Describable` instance.
/// Demonstrates a trait-constrained function using `with`.
def describeItem(x: a): String with Describable[a] =
    Describable.describeTwice(x)

// ----------------------------------------------------------------------------
// 5. Structs & Companion Module (Internal Mutability with Regions)
// ----------------------------------------------------------------------------

/// A mutable struct allocated in a region `r`. `name` is immutable; `age`
/// can be updated in place via `->`.
struct Person[r] {
    name: String,
    mut age: Int32
}

/// Companion module for `Person`.
mod Person {
    /// Allocates a `Person` named `name` at `age` in region `rc`, ages them
    /// by one year in place, and returns their name and new age.
    pub def makeAndAge(name: String, age: Int32, rc: Region[r]): (String, Int32) \ r = {
        let p = new Person @ rc { name = name, age = age };
        p->age = p->age + 1;
        (p->name, p->age)
    }
}

/// Demonstrates constructing a `Person` in a region and mutating one of its
/// fields.
def structDemo(): (String, Int32) = region rc {
    Person.makeAndAge("Alice", 30, rc)
}

// ----------------------------------------------------------------------------
// 6. Algebraic Effects & Handlers
// ----------------------------------------------------------------------------

/// An effect for reading and writing a single `Int32` cell of state.
eff StateEff {
    def get(): Int32
    def put(v: Int32): Unit
}

/// An effect for emitting a log message.
eff LogEff {
    def log(msg: String): Unit
}

/// Reads the current `StateEff` value, logs it via `LogEff`, stores the
/// incremented value, and returns it. Requires both effects.
def incrementState(): Int32 \ {StateEff, LogEff} = {
    let curr = StateEff.get();
    LogEff.log("Current state is ${curr}");
    StateEff.put(curr + 1);
    curr + 1
}

/// Runs `incrementState` under nested handlers: an inner handler for
/// `StateEff` seeded at `10`, wrapped in an outer handler that discards
/// `LogEff` messages.
def runEffectDemo(): Int32 = {
    run {
        run incrementState() with handler StateEff {
            def get(k) = k(10)
            def put(_, k) = k()
        }
    } with handler LogEff {
        def log(_, k) = k()
    }
}

// ----------------------------------------------------------------------------
// 7. Java Interoperability & Exception Handling
// ----------------------------------------------------------------------------

/// Calls a static Java method (`Math.abs`) and builds a string via a mutable
/// Java `StringBuilder`. Both require the `IO` effect.
def javaInteropDemo(): String \ IO = {
    let _absVal = Math.abs(-42);
    let sb = new StringBuilder();
    sb.append("Hello from ");
    sb.append("Java Interop!");
    sb.toString()
}

/// Demonstrates catching a Java exception with `try`/`catch`. No exception
/// is actually thrown here, so the `try` branch always succeeds.
def javaExceptionDemo(): String \ IO = {
    try {
        let sb = new StringBuilder();
        sb.append("Try/Catch demo");
        sb.toString()
    } catch {
        case _ex: Exception => "Caught Java Exception"
    }
}

// ----------------------------------------------------------------------------
// 8. First-Class Datalog / Fixpoint Computations
// ----------------------------------------------------------------------------

/// Builds a Datalog program computing graph reachability (`Path`) over a
/// fixed set of `Edge` facts, solves it, and queries out every reachable
/// pair.
def datalogDemo(): Vector[(Int32, Int32)] = {
    let p = #{
        Edge(1, 2).
        Edge(2, 3).
        Edge(3, 4).
        Path(x, y) :- Edge(x, y).
        Path(x, z) :- Path(x, y), Edge(y, z).
    };
    let resolved = solve p;
    query resolved select (x, y) from Path(x, y)
}

// ----------------------------------------------------------------------------
// 9. Functions, Currying, Pipelines, Lambdas & Tail Recursion
// ----------------------------------------------------------------------------

/// Adds `x` and `y`.
def add(x: Int32, y: Int32): Int32 = x + y

/// Returns a function that multiplies its argument by `x`. Demonstrates
/// currying via a returned closure.
def curriedMultiply(x: Int32): Int32 -> Int32 = y -> x * y

/// Returns `x` unchanged. Polymorphic over any type `a`.
def genericIdentity(x: a): a = x

/// Applies `f` to `x`. Demonstrates a higher-order function taking another
/// function as an argument.
def higherOrderDemo(f: Int32 -> Int32, x: Int32): Int32 = f(x)

/// Adds `5` to `x`, then doubles the result, using the `|>` pipeline
/// operator to chain lambdas.
def pipelineDemo(x: Int32): Int32 =
    x |> (n -> n + 5) |> (n -> n * 2)

/// Adds `a` and `b` by piping the tuple `(a, b)` into `add` via the binary
/// pipeline operator `||>`.
def binaryPipelineDemo(a: Int32, b: Int32): Int32 =
    (a, b) ||> add

/// Computes `n!` tail-recursively, accumulating the running product in
/// `acc`.
def factorial(n: Int64, acc: Int64): Int64 =
    if (n <= 1i64) acc else factorial(n - 1i64, n * acc)

// ----------------------------------------------------------------------------
// 10. Records (Construction, Extension, Restriction, Update)
// ----------------------------------------------------------------------------

/// Builds a record, extends it with a new field (`+z`), updates an existing
/// field (`x`), and restricts away a field (`-y`), returning the values
/// visible before restriction.
def recordDemo(): (Int32, String, Float64) = {
    let base = { x = 10, y = "Flix" };
    let extended = { +z = 3.14 | base };
    let updated = { x = 20 | extended };
    let _restricted = { -y | updated };
    (updated#x, updated#y, updated#z)
}

/// Projects the `x` field out of any record that has one, regardless of
/// what other fields (`rest`) it carries.
def getX(r: {x = a | rest}): a = r#x

// ----------------------------------------------------------------------------
// 11. Pattern Matching & Guards
// ----------------------------------------------------------------------------

/// Describes `x` by matching on its `SimpleResult` case, including a guard
/// (`if v > 100`) and specific literal patterns (`0`, `1`) matched before
/// the catch-all `v` case.
def patternMatchDemo(x: SimpleResult[Int32, String]): String = match x {
    case SimpleResult.Ok(v) if v > 100 => "Large Ok: ${v}"
    case SimpleResult.Ok(0)             => "Zero Ok"
    case SimpleResult.Ok(1)             => "One Ok"
    case SimpleResult.Ok(v)             => "Normal Ok: ${v}"
    case SimpleResult.Err(msg)          => "Error: ${msg}"
}

/// Describes `p` by matching on a 3-tuple pattern, including a literal
/// component (`0`), a variable bound alongside a literal (`true`), and a
/// wildcard combined with a literal (`false`).
def tuplePatternMatch(p: (Int32, String, Bool)): String = match p {
    case (0, _, _)          => "Zero fst"
    case (n, str, true)     => "${str} true (${n})"
    case (_, _, false)      => "False thd"
}

// ----------------------------------------------------------------------------
// 12. Literals, Collections & Monadic For (forM)
// ----------------------------------------------------------------------------

/// Declares a value of every primitive and literal-syntax type Flix
/// supports (unit, bool, char, the sized int/float families, `BigInt`,
/// `BigDecimal`, string interpolation, and a regex literal), and returns
/// the `Int32` one.
def literalsDemo(): Int32 = {
    let _unitVal: Unit = ();
    let _boolVal: Bool = true;
    let _charVal: Char = 'F';
    let _i8Val: Int8 = 8i8;
    let _i16Val: Int16 = 16i16;
    let i32Val: Int32 = 32i32;
    let _i64Val: Int64 = 64i64;
    let _biVal: BigInt = 128ii;
    let _f32Val: Float32 = 3.14f32;
    let _f64Val: Float64 = 6.28;
    let _bdVal: BigDecimal = 12.34ff;
    let _strVal: String = "Hello ${42}";
    let _regexVal: Regex = regex"^[a-zA-Z]+$";
    i32Val
}

/// Builds a `List`, `Vector`, `Set`, and `Map` via `#{...}` literal syntax,
/// then uses `forM` to sequence two `Option` values monadically and conses
/// the result onto the list if both are present.
def collectionsDemo(): List[Int32] = {
    let list1 = List#{1, 2, 3};
    let _vec1 = Vector#{4, 5, 6};
    let _set1 = Set#{1, 2, 3};
    let _map1 = Map#{1 => "one", 2 => "two"};

    // Monadic forM with Option
    let optResult: Option[Int32] = forM (
        x <- Some(10);
        y <- Some(20)
    ) yield x + y;

    match optResult {
        case None    => list1
        case Some(v) => v :: list1
    }
}

// ----------------------------------------------------------------------------
// 13. Concurrency, Channels, Select & Parallelism (par yield)
// ----------------------------------------------------------------------------

/// Spawns a thread in region `rc` that sends `42` over a buffered channel,
/// then receives it back on the main thread.
def concurrencyDemo(): Int32 \ {Chan, NonDet} = region rc {
    let (s, r) = Channel.buffered(1);
    spawn {
        Channel.send(42, s)
    } @ rc;
    Channel.recv(r)
}

/// Spawns a thread that sends a message on one of two channels, then uses
/// `select` to receive from whichever channel becomes ready first.
def selectDemo(): String \ {Chan, NonDet} = region rc {
    let (s1, r1) = Channel.buffered(1);
    let (_s2, r2) = Channel.buffered(1);
    spawn {
        Channel.send("Hello Select", s1)
    } @ rc;
    select {
        case msg <- recv(r1) => msg
        case msg <- recv(r2) => msg
    }
}

/// Evaluates two independent expressions in parallel via `par`/`yield` and
/// pairs up their results.
def parYieldDemo(): (Int32, String) = {
    par (x <- 1 + 2; y <- "hello world")
        yield (x, y)
}

// ----------------------------------------------------------------------------
// 14. Imperative Constructs, Foreach Loops & References in Regions
// ----------------------------------------------------------------------------

/// Sums the elements of a mutable `Array` and, separately, the elements of
/// a `List` that pass a filter, accumulating both into one `Ref` cell.
def imperativeDemo(): Int32 = region rc {
    let total = Ref.fresh(rc, 0);
    let arr = Array#{1, 2, 3, 4, 5} @ rc;

    // foreach loop
    foreach (item <- arr) {
        let current = Ref.get(total);
        Ref.put(current + item, total)
    };

    // foreach with filter condition
    foreach (item <- List#{10, 20, 30}; if item > 15) {
        let current = Ref.get(total);
        Ref.put(current + item, total)
    };

    Ref.get(total)
}

// ----------------------------------------------------------------------------
// 15. Lazy Evaluation, Unsafe & Checked/Unchecked Casts
// ----------------------------------------------------------------------------

/// Builds a `lazy` expression, `force`s it, and evaluates the result inside
/// an `unsafe IO` block. Demonstrates deferred evaluation and an unchecked
/// effect cast.
def lazyAndCastsDemo(): Int32 = {
    let lazyVal = lazy (10 + 20);
    let forcedVal: Int32 = force lazyVal;
    let safeVal = unsafe IO {
        println("Evaluating inside unsafe IO block");
        forcedVal
    };
    safeVal
}

// ----------------------------------------------------------------------------
// 16. Specialization Coverage
//
// The sections above exercise the language broadly, but leave two symbol kinds
// unreachable and specialize each polymorphic function only once or twice. This
// section closes both gaps, so a fresh-id trace covers every symbol kind that
// can carry a `$id` into a generated JVM name.
// ----------------------------------------------------------------------------

/// A struct polymorphic in a *value* type, not just a region. Specializing
/// it at several element types is what mints fresh `StructSym`s;
/// `Person[r]` above is parameterized only over its region, so it never
/// needs one.
struct DemoBox[a, r] {
    mut value: a
}

/// Companion module for `DemoBox`.
mod DemoBox {
    /// Boxes `v` in region `rc`, writes it back to the same field, and
    /// returns it. Instantiating this at many types for `v` is what drives
    /// specialization; see `boxDemo`.
    pub def roundTrip(v: a, rc: Region[r]): a \ r = {
        let b = new DemoBox @ rc { value = v };
        b->value = v;
        b->value
    }
}

/// Calls `DemoBox.roundTrip` at ten distinct element types. Each call is a
/// distinct specialization of both `DemoBox` and `roundTrip`.
def boxDemo(): String = region rc {
    let i = DemoBox.roundTrip(42, rc);
    let s = DemoBox.roundTrip("boxed", rc);
    let b = DemoBox.roundTrip(true, rc);
    let c = DemoBox.roundTrip('x', rc);
    let f = DemoBox.roundTrip(2.5f64, rc);
    let l = DemoBox.roundTrip(7i64, rc);
    let col = DemoBox.roundTrip(Color.Blue, rc);
    let opt = DemoBox.roundTrip(Some(1), rc);
    let lst = DemoBox.roundTrip("a" :: "b" :: Nil, rc);
    let tup = DemoBox.roundTrip((1, "t"), rc);
    "${i}/${s}/${b}/${c}/${f}/${l}/${col}/${opt}/${List.length(lst)}/${fst(tup)}"
}

/// Instantiates two Java anonymous classes (`Supplier` and `Comparator`).
/// This is the only construct that mints an `AnonClassSym`, which becomes
/// an `Anon$<id>` class in the output. Note that the implemented method
/// cannot be named `run`, since that is a Flix keyword.
def anonClassDemo(): String \ IO = {
    let supplier = new Supplier[String] {
        def get(_this: Supplier[String]): String = "from-anon-class"
    };
    let cmp = new Comparator[String] {
        def compare(_this: Comparator[String], a: String, b: String): Int32 =
            String.length(a) - String.length(b)
    };
    let v = supplier.get();
    let c = cmp.compare("aa", "b");
    "${v}/${c}"
}

/// A polymorphic function instantiated at many distinct types below. This
/// is what drives the specialization count: the same source def becomes one
/// generated class per type it is used at, and today those are told apart
/// only by `$id`.
def describeShape(x: a, label: String): String with ToString[a] =
    "${label}=${x}"

/// Calls `describeShape` at twelve distinct type arguments, forcing twelve
/// separate specializations of the same source def.
def manyInstantiations(): Int32 = {
    let parts =
        describeShape(1, "i32") ::
        describeShape(2i8, "i8") ::
        describeShape(3i16, "i16") ::
        describeShape(4i64, "i64") ::
        describeShape(5.0f32, "f32") ::
        describeShape(6.0f64, "f64") ::
        describeShape('c', "char") ::
        describeShape(true, "bool") ::
        describeShape("str", "string") ::
        describeShape(Color.Red, "enum") ::
        describeShape(Some(9), "option") ::
        describeShape(1 :: 2 :: Nil, "list") ::
        Nil;
    List.length(parts)
}

/// Returns two lambdas lifted out of *this* definition rather than inlined
/// into the library function they are passed to. Both capture `n`, so they
/// become closures, and both escape by being returned, so neither can be
/// optimized away. This is what makes the definition able to test whether
/// inserting a lambda renumbers the ones after it: elsewhere in this file
/// the lambdas are inlined into specializations of `List.map` and so are
/// lifted out of those instead.
def liftedClosures(n: Int32): (Int32 -> Int32, Int32 -> Int32) =
    let f = x -> x + n;
    let g = y -> y * n;
    (f, g)

/// Obtains the two closures from `liftedClosures` and combines their
/// results on the same input.
def liftedClosuresDemo(): Int32 =
    let (f, g) = liftedClosures(3);
    f(10) + g(10)

/// Applies the same stdlib higher-order functions (`List.map`,
/// `List.filter`, `Vector.map`) at many distinct element types, so the
/// library defs they call are specialized repeatedly rather than once.
def repeatedStdlibDemo(): Int32 = {
    let a = List.map(x -> x + 1, 1 :: 2 :: 3 :: Nil) |> List.length;
    let b = List.map(x -> x + 1i64, 1i64 :: 2i64 :: Nil) |> List.length;
    let c = List.map(x -> "${x}!", "p" :: "q" :: Nil) |> List.length;
    let d = List.map(x -> not x, true :: false :: Nil) |> List.length;
    let e = List.map(x -> x * 2.0f64, 1.0f64 :: 2.0f64 :: Nil) |> List.length;
    let f = List.filter(x -> x > 1, 1 :: 2 :: 3 :: Nil) |> List.length;
    let g = List.filter(x -> String.length(x) > 1, "y" :: "zz" :: Nil) |> List.length;
    let h = Vector.map(x -> x + 1, Vector#{1, 2, 3}) |> Vector.length;
    let i = Vector.map(x -> "${x}", Vector#{'a', 'b'}) |> Vector.length;
    a + b + c + d + e + f + g + h + i
}

// ----------------------------------------------------------------------------
// Main Entry Point
// ----------------------------------------------------------------------------

/// Entry point. Runs every demo function above in turn and prints its
/// result, exercising each language construct and symbol kind covered by
/// this file.
def main(): Unit \ {Chan, NonDet, IO} = {
    use HelperModule.helper;
    use HelperModule.moduleValue;

    println("=== Flix All Constructs Demonstration ===");

    // 1. Modules & Imports
    let h = helper();
    let mv = moduleValue();
    println("Module Helper: ${h}, Module Value: ${mv}");

    // 2. Structs & Regions
    let (name, age) = structDemo();
    println("Struct Person: ${name}, Age: ${age}");

    // 3. Effects & Handlers
    let effRes = runEffectDemo();
    println("Effect result: ${effRes}");

    // 4. Java Interop & Exceptions
    let javaStr = javaInteropDemo();
    let javaExStr = javaExceptionDemo();
    println("Java Interop: ${javaStr}");
    println("Java Try/Catch: ${javaExStr}");

    // 5. Datalog
    let paths = datalogDemo();
    println("Datalog paths found: ${Vector.length(paths)}");

    // 6. Traits & Polymorphism
    let desc1 = describeItem(Color.Green);
    let desc2 = describeItem(123);
    let desc3 = describeItem(Some("nested"));
    println("Trait describe: ${desc1}");
    println("Trait describe: ${desc2}");
    println("Trait describe: ${desc3}");

    // 7. Enums & Restrictable Enums
    let shape = Shape.Circle(5.0);
    let area = shapeArea(shape);
    println("Restrictable Shape area: ${area}");

    // 8. Functions, Currying & Pipelines
    let pipeRes = pipelineDemo(10);
    let binPipeRes = binaryPipelineDemo(20, 22);
    let multFn = curriedMultiply(6);
    let multRes = multFn(7);
    let factRes = factorial(5i64, 1i64);
    println("Pipeline: ${pipeRes}");
    println("Binary Pipeline: ${binPipeRes}");
    println("Curried Multiply: ${multRes}");
    println("Factorial(5): ${factRes}");

    // 9. Records & Pattern Matching
    let (rx, ry, rz) = recordDemo();
    let polyRec = getX({x = 99, extra = "info"});
    let patMatchRes = patternMatchDemo(SimpleResult.Ok(150));
    let tupleMatchRes = tuplePatternMatch((1, "Status", true));
    println("Record: x=${rx}, y=${ry}, z=${rz}");
    println("Poly record getX: ${polyRec}");
    println("Pattern match: ${patMatchRes}");
    println("Tuple pattern match: ${tupleMatchRes}");

    // 10. Literals & Collections & Comprehensions
    let _lit = literalsDemo();
    let colList = collectionsDemo();
    println("Collections result: ${colList}");

    // 11. Imperative & References
    let impRes = imperativeDemo();
    println("Imperative total: ${impRes}");

    // 12. Lazy & Casts
    let lazyRes = lazyAndCastsDemo();
    println("Lazy/Casts result: ${lazyRes}");

    // 13. Parallelism & Concurrency & Select
    let (py1, py2) = parYieldDemo();
    println("Par yield: (${py1}, ${py2})");
    let chanVal = concurrencyDemo();
    println("Channel received: ${chanVal}");
    let selectVal = selectDemo();
    println("Select received: ${selectVal}");

    // 16. Specialization Coverage
    println("DemoBox (polymorphic struct): ${boxDemo()}");
    println("Anon class: ${anonClassDemo()}");
    println("Instantiations: ${manyInstantiations()}");
    println("Lifted closures: ${liftedClosuresDemo()}");
    println("Repeated stdlib: ${repeatedStdlibDemo()}");

    println("=== Done ===")
}
