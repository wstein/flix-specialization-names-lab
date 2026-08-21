# Comparative-reference ports

`ports/{scala,java,kotlin,clojure}/` are four sibling ports of
`src/AllConstructs.flix`, one per language, added as a **comparative
reference**: showing how each JVM language expresses the same constructs
Flix's own demo exercises. They extend the lab's documentation, not its
method.

## What this is not

**These ports produce no evidence about specialization-name stability.**
The lab's actual method — `scripts/class-id-report`, `scripts/edit-resistance`,
`scripts/collision-stress`, `scripts/duplicate-decl-stress`, and
`fixtures/` — measures how the *Flix compiler* names the classes it
generates when it specializes polymorphic code. None of that has a
counterpart here, for one unavoidable reason:

**JVM generics erase.** Scala, Java, and Kotlin all compile a generic
method or class to exactly *one* class file, regardless of how many types
it is called at. `DemoBox<A>` is one class whether it is instantiated at
ten types or ten thousand; there is nothing for a `class-id-report`
equivalent to census, because there is no per-instantiation class to find.
Flix's specializer instead mints a distinct class per specialization —
that difference *is* what the lab studies, and it has no JVM-native
analogue to compare against.

Clojure sharpens this further: it is dynamically typed and never had
static generics to begin with, so there is no erasure story to tell either
— `demo-box-round-trip` and `describe-shape` are single, ordinary
functions, and every call is the same dynamic dispatch regardless of
argument type. See `docs/adr/0002-ports-are-comparative-not-experimental.md`
for the boundary this implies.

No FP/effect library (cats-effect, ZIO, Arrow-kt, `core.async`) is used for
any effect or concurrency approximation below, to keep the four ports
comparably weighted and dependency-light, matching the rest of the repo.
Datalog (`datalogDemo`, Flix section 8) is excluded from every port — it
has no meaning outside Flix's fixpoint engine.

## Per-section mapping

Clean, native mappings needing no approximation, across all four
languages: modules/namespaces, ADTs via closed sum types + pattern
matching, Java interop, currying/pipelines, pattern matching + guards,
literals/collections, imperative loops, anonymous classes (Scala, Java,
Kotlin — via each language's own anonymous-class/anonymous-object syntax;
Clojure via `reify`).

| Construct | Scala | Java | Kotlin | Clojure |
|---|---|---|---|---|
| Type alias | native `type` | **no equivalent** | native `typealias` | **no equivalent** |
| Restrictable enum | sealed-subset trait (narrower sealed hierarchy, not open-world) | sealed interface subset | sealed interface/class subset | a keyword validated against a `#{...}` set *at construction, at runtime* — dynamic typing has no compile-time restriction to approximate |
| Typeclass (`Describable`) | native `given`/`using` — real typeclasses | dictionary passing (instance passed explicitly) | dictionary passing (plain instance objects) | native `defprotocol` — dispatches on runtime type, no dictionary needed |
| Region / struct-in-region | block scoping | `AutoCloseable` + try-with-resources | `Closeable` + `use { }` | an atom scoped to a `let` block |
| Effect handlers | hand-rolled interpreter pattern (trait + explicit handler) | same, via functional interfaces | same, via interfaces (not coroutine `context`) | protocols + `reify`, not `core.async` |
| Row polymorphism (`getX`) | a `HasX[A]` interface each record implements | same `HasX<A>` interface | same `HasX<A>` interface | **no approximation needed** — maps are already structurally open |
| `forM` (sequencing two optional sources) | for-comprehension over `Option` | nested `Optional.flatMap` | nullable `?.let` chain | `and`/`when` (Clojure's `some->`/`some->>` only thread *one* value through a pipeline, not two independent sources) |
| `lazy`/`force` | native `lazy val` | `Supplier` + manual memoization | native `by lazy { }` | native `delay`/deref |
| Unsafe effect-cast (`unsafe IO`) | **no equivalent** — no static effect tracking to bypass | **no equivalent** | **no equivalent** | **no equivalent**; dynamic typing also means no static cast to approximate |
| Concurrency (channel/select/par-yield) | `BlockingQueue` + `Future.firstCompletedOf` + `Future` zip | `BlockingQueue` + `CompletableFuture` on a virtual-thread executor | `kotlinx.coroutines` `Channel`/`select`/`async` — **near-native, not approximated** | `BlockingQueue` + `CompletableFuture` on a virtual-thread executor, deliberately not `core.async` |
| Foreach with filter | `for ... if` | `for` + `if` | `for` + `if` | `doseq` with `:when` — a direct, unapproximated syntactic match |
| Specialization coverage (§16) | generics erase to one class regardless of instantiation count | same erasure story | same erasure story | **no erasure story at all** — never had static generics to specialize |

Java's `StructuredTaskScope` (JEP 453) would be a closer structural match
for par/yield than `CompletableFuture`, but it remains a preview API in
JDK 21 requiring `--enable-preview` at both build and run time; the Java
port uses `CompletableFuture` instead to stay buildable and runnable
without that flag. Kotlin 2.1+ also supports `is X if cond ->` guard
syntax directly inside a subject `when`; the Kotlin port uses nested
`when` instead, for broad-version compatibility.

## Testing bar

Each port has one test per demo function, verifying its **observable
return value** against the same expected values `test/TestMain.flix` uses
on the Flix side — not a bytecode census (there is nothing meaningful to
census under JVM erasure, and nothing at all under Clojure's dynamic
typing) and not output-string parity with Flix's `.out` snapshot. Each
port's tests are self-consistent, checked against that language's own
values, independent of the other three ports and of Flix.

Frameworks: **munit** (Scala), **JUnit 5** (Java, with a
`@ParameterizedTest` sweep over `describeShape`'s instantiations),
**JUnit 5 + kotlin-test** (Kotlin), **`clojure.test`** (Clojure).

## Where to look

- `ports/scala/README.md`, `ports/java/README.md`, `ports/kotlin/README.md`,
  `ports/clojure/README.md` — build/test commands and a per-language recap
  of what's native, approximated, or missing in that port specifically.
- `docs/adr/0002-ports-are-comparative-not-experimental.md` — the boundary
  decision: these ports feed nothing in `scripts/` and answer no question
  about specialization-name stability.
