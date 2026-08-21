# AllConstructs — Java port

A comparative-reference port of `src/AllConstructs.flix`. See
[`docs/ports.md`](../../docs/ports.md) at the repository root for what this
is, what it deliberately does not measure, and the full per-language mapping
table.

## Commands

- `mvn -q exec:java` — run `Main`
- `mvn test` — run the JUnit 5 test suite
- `mvn fmt:format` — format sources in place (google-java-format)

## What's native, approximated, or missing

Java 21's sealed interfaces + records + pattern-matching `switch` give
ADTs, the restrictable-enum approximation, and pattern matching a mapping
just as clean as Flix's own -- and section 7 (Java interop) needs no
approximation at all, since this port already is Java.

Named approximations, each documented at its definition site:

- **Typeclasses** (`Describable`, section 4) → dictionary passing: each
  instance is a plain implementation of the interface, passed to
  `describeItem` explicitly. Java has no native typeclass resolution.
- **Regions** (`Person`, `DemoBox`, section 5 & 16) → `AutoCloseable` +
  try-with-resources. Nothing statically checks that a value doesn't
  escape its `try` block, unlike Flix's region typing.
- **Effect handlers** (`Effects`, section 6) → a hand-rolled interpreter
  pattern via functional interfaces, deliberately without an effect
  library.
- **Row polymorphism** (`getX`, section 10) → a `HasX<A>` interface each
  record explicitly implements.
- **Concurrency** (`Concurrency`, section 13) → `ArrayBlockingQueue` +
  `CompletableFuture` on a virtual-thread executor. JDK 21 also ships
  `StructuredTaskScope` (JEP 453) as a closer structural match, but only
  as a preview API requiring `--enable-preview` at both build and run
  time; `CompletableFuture` keeps this port buildable without that flag.
- **Lazy evaluation** (`LazyAndCasts`, section 15) → a memoizing
  `Supplier`, since Java has no first-class lazy value.

Two constructs have **no equivalent at all**, stated rather than faked
(see `package-info.java`):

- **Type aliases** (section 2) — Java has no type-level alias mechanism.
- **Flix's `unsafe IO` block** (section 15) — it discharges a *static*
  effect-typing obligation Java method signatures never carry in the
  first place, so there is nothing to unsafely bypass.

Section 16 ("Specialization Coverage") is the comparison's payload: JVM
generics erase, so `DemoBox<A>` and `describeShape` each compile to a
*single* class/method regardless of how many types they're called at,
unlike Flix's one-class-per-specialization output. See
[`docs/ports.md`](../../docs/ports.md) for the full per-language table.
