# 2. The comparative-reference ports are not part of the lab's method

## Status

Accepted.

## Context

`ports/{scala,java,kotlin,clojure}/` are four sibling ports of
`src/AllConstructs.flix`, one per JVM language, showing how each expresses
the same constructs Flix's own demo exercises — ADTs, typeclasses, structs,
effect handlers, records, pattern matching, concurrency, and (section 16)
a polymorphic struct and a def called at many distinct type arguments.

A future contributor could reasonably read "section 16: Specialization
Coverage" ported four times and assume the ports feed the lab's actual
measurement somehow — a `class-id-report` equivalent, a fifth column on
the width-nesting table in `docs/adr/0001-content-addressed-specialization-names.md`,
evidence for or against the fork's naming scheme. They would be wrong, and
the mistake is easy enough to make that it is worth ruling out explicitly
rather than leaving it implicit in what the ports simply don't contain.

## Decision

The ports are a **comparative reference only**. Specifically:

- They produce **no evidence** for or against the content-addressed vs.
  counter-derived naming question `docs/adr/0001-content-addressed-specialization-names.md`
  and the fork's own ADRs address. Nothing here is cited by, or feeds, that
  question.
- They are **not consumed by `scripts/`**. `class-id-report`,
  `edit-resistance`, `collision-stress`, `duplicate-decl-stress`, and
  `fixtures-check`/`fixtures-build` all operate on Flix source and Flix
  compiler output exclusively. None of them, now or prospectively, reads
  anything under `ports/`.
- They exist for **pedagogy and comparison**: what does Flix's `Describable`
  trait look like as a Scala `given`, a Java dictionary-passed interface, a
  Kotlin interface, or a Clojure protocol? What survives translation
  cleanly, and what needs an approximation, and what has no equivalent at
  all? `docs/ports.md` is where those answers live.

The reason a `class-id-report`-equivalent for the ports would be a category
error, not just unbuilt: **JVM generics erase.** `DemoBox<A>` compiles to
one class regardless of how many types it is instantiated at, so there is
no per-specialization class for a census to find, in any of the three
statically-typed ports. Clojure has no static generics to erase in the
first place — same absence of evidence, for a different reason. A tool
built to census `ports/` output would report a constant "one class per
generic def," which is not a measurement of anything; it is a restatement
of how the JVM (or, for Clojure, the absence of a type system) already
works.

## Consequences

- `docs/ports.md`, and each `ports/*/README.md`, state the erasure/no-
  generics boundary up front, not as a footnote — a reader should not have
  to infer it from what's missing.
- A contributor proposing a JVM-side "specialization census" tool should
  read this ADR first: the premise doesn't hold, so the tool would need a
  different question to answer, not just a different implementation.
- The ports' own test suites are self-consistent (checked against each
  language's own expected values), not cross-checked against Flix's
  `test/AllConstructs.out` snapshot or against each other — there is no
  claim of behavioral parity beyond "the same demo, expressed idiomatically
  in each language."
