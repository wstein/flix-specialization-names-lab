# AllConstructs — Kotlin port

A comparative-reference port of `src/AllConstructs.flix`. See
[`docs/ports.md`](../../docs/ports.md) at the repository root for what this
is, what it deliberately does not measure, and the full per-language mapping
table.

## Commands

- `./gradlew run` — run `main`
- `./gradlew test` — run the JUnit 5 test suite
- `./gradlew ktlintFormat` — format sources in place
- `./gradlew ktlintCheck` — check formatting without writing

## What's native, approximated, or missing

Kotlin is the one port where a whole section needs **no approximation**:
`kotlinx.coroutines`' `Channel`, `select { }`, and `async`/`await`
(`Concurrency.kt`, section 13) map almost 1:1 onto Flix's channel,
`select`, and `par`/`yield` constructs. Row polymorphism (`getX`, via a
`HasX<A>` interface) and typeclasses (`Describable`, via dictionary
passing) still need the same approximations Scala and Java do, since
Kotlin has neither structural typing nor native typeclass resolution.
Kotlin's nullable types (`T?`) stand in for Flix's `Option[T]`
(`NullableDescribable`) — genuinely idiomatic Kotlin, not an
approximation.

Other named approximations, each documented at its definition site:

- **Restrictable enums** (`Shape`, section 3) → an ordinary `sealed
  interface`. Flix's restrictable enums allow open-world subsetting via
  `choose`; Kotlin's sealed hierarchy is closed once sealed.
- **Regions** (`Person`, `DemoBox`, section 5 & 16) → `Closeable` +
  `use { }`. Nothing statically checks that a value doesn't escape its
  block, unlike Flix's region typing.
- **Effect handlers** (`Effects.kt`, section 6) → a hand-rolled
  interpreter pattern, deliberately not `kotlinx.coroutines`' structured
  concurrency or an effect library.

One construct has **no equivalent at all**, stated rather than faked:
Flix's `unsafe IO { ... }` (section 15) discharges a *static*
effect-typing obligation that Kotlin function signatures never carry in
the first place.

Section 16 ("Specialization Coverage") is the comparison's payload: JVM
generics erase, so `DemoBox<A>` and `describeShape` each compile to a
*single* class/function regardless of how many types they're called at,
unlike Flix's one-class-per-specialization output. See
[`docs/ports.md`](../../docs/ports.md) for the full per-language table.
