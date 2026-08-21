# AllConstructs — Scala port

A comparative-reference port of `src/AllConstructs.flix`. See
[`docs/ports.md`](../../docs/ports.md) at the repository root for what this
is, what it deliberately does not measure, and the full per-language mapping
table.

## Commands

- `sbt run` — run `Main`
- `sbt test` — run the munit test suite
- `sbt scalafmtAll` — format sources in place
- `sbt scalafmtCheckAll` — check formatting without writing

## What's native, approximated, or missing

Scala 3 covers the most ground of the four ports: `given` instances give
typeclasses (`Describable`) and conditional instances a *native* mapping,
and type aliases (`TypeAliases.scala`) need no approximation at all.

Named approximations, each documented at its definition site:

- **Restrictable enums** (`Shape`, section 3) → an ordinary `sealed trait`.
  Flix's restrictable enums let callers be polymorphic over an *open*
  subset of cases (`choose`); Scala's sealed hierarchy is closed once
  sealed, so there is no open-world subsetting to reproduce.
- **Regions** (`Person`, `DemoBox`, section 5 & 16) → plain block scoping.
  Nothing statically checks that a value doesn't escape its block, unlike
  Flix's region typing.
- **Effect handlers** (`StateHandler`/`LogHandler`, section 6) → a
  hand-rolled interpreter pattern, deliberately without an effect library
  (cats-effect, ZIO).
- **Row polymorphism** (`getX`, section 10) → a `HasX[A]` interface each
  record explicitly implements, rather than Flix's implicit structural
  typing.
- **Concurrency** (channels/select/par-yield, section 13) →
  `java.util.concurrent.BlockingQueue` + `scala.concurrent.Future`,
  deliberately plain rather than a reactive library.

One construct has **no equivalent at all**, stated rather than faked:
Flix's `unsafe IO { ... }` (section 15) discharges a *static* effect-typing
obligation that Scala's function signatures never carry in the first place
-- there is nothing to unsafely bypass.

Section 16 ("Specialization Coverage") is the comparison's payload: JVM
generics erase, so `DemoBox[A]` and `describeShape[A]` each compile to a
*single* class/method regardless of how many types they're called at,
unlike Flix's one-class-per-specialization output. See
[`docs/ports.md`](../../docs/ports.md) for the full per-language table.
