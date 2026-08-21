# AllConstructs — Clojure port

A comparative-reference port of `src/AllConstructs.flix`. See
[`docs/ports.md`](../../docs/ports.md) at the repository root for what this
is, what it deliberately does not measure, and the full per-language mapping
table.

## Commands

- `lein run` — run `-main`
- `lein test` — run the `clojure.test` suite
- `lein cljfmt fix` — format sources in place
- `lein cljfmt check` — check formatting without writing

## What's native, approximated, or missing

Clojure is the deliberately different data point among the four ports:
dynamically typed, with no compile-time generics at all. That reshapes
several sections that need real approximation in Scala/Java/Kotlin into
things Clojure does natively instead:

- **Typeclasses** (`describable.clj`, section 4) → a `defprotocol`,
  dispatching on runtime type exactly the way Flix's trait dispatch
  does. No dictionary passing needed.
- **Row polymorphism** (`records.clj`, section 10) → nothing at all.
  Clojure maps are already structurally open; `:x` looks a key up on any
  map that has one, so there's no `HasX`-style interface to write.
- **Foreach with a filter** (`imperative.clj`, section 14) →
  `doseq`'s `:when` modifier, a direct syntactic match for Flix's
  `foreach (item <- xs; if cond)`.
- **Lazy evaluation** (`lazyandcasts.clj`, section 15) → native
  `delay`/`force`.
- **Anonymous classes** (`anonclass.clj`, section 16) → `reify`, the
  same construct Flix's demo exercises, reached through different syntax.

Named approximations, each documented at its definition site:

- **Restrictable enums** (`adts.clj`, section 3) → a keyword validated
  against a defined `#{...}` set *at construction, at runtime* — dynamic
  typing means there is no compile-time restriction to approximate in
  the first place.
- **Regions** (`structs.clj`, section 5) → an atom scoped to a `let`
  block. Nothing statically checks that a value doesn't escape it.
- **Effect handlers** (`effects.clj`, section 6) → protocols + `reify`,
  deliberately not `core.async` or an effect library.
- **`forM`** (`literals.clj`, section 12) → `and`/`when` combining two
  independent optional sources, since core Clojure's `some->`/`some->>`
  only thread a *single* value through a pipeline.
- **Concurrency** (`concurrency.clj`, section 13) →
  `java.util.concurrent.ArrayBlockingQueue` + `CompletableFuture` on a
  virtual-thread executor, deliberately not `core.async`.

Two constructs have **no equivalent at all**, stated rather than faked:

- **Type aliases** (section 2) — Clojure has no type-level aliasing.
- **Flix's `unsafe IO` block and checked/unchecked casts** (section 15)
  — there is no static effect obligation to discharge and no static type
  to cast away; a Clojure call is checked at the point of use.

Section 16 ("Specialization Coverage") is where Clojure diverges most
sharply from the other three ports: JVM generics erase for Scala/Java/
Kotlin, but Clojure never had static generics to specialize *at all* —
`demo-box-round-trip` and `describe-shape` are single, ordinary
functions, and every call is the same dynamic dispatch regardless of
argument type. There is no erasure story to document, because there was
nothing generic to compile away in the first place. See
[`docs/ports.md`](../../docs/ports.md) for the full per-language table.
