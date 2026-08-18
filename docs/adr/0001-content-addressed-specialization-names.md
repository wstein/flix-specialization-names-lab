# 1. How this lab measures content-addressed specialization names

## Status

Accepted, and **scoped to method**. The naming scheme this lab measures is
decided in the compiler fork, not here:

- **`docs/adr/0002-content-addressed-generated-names.md`** (in `wstein/flix-fork`)
  — the key contract, the choice of SHA-256 and base-36, the width policy and why
  there is no floor, uniform-width enforcement, the format version, and the
  collision policy.
- **`docs/adr/0001-source-identity-vs-generated-name-identity.md`** (same repo) —
  what a *declaration's* identity is, and what follows when two declarations
  cannot be told apart.

Those decisions live beside the code that implements them and the tests that
enforce them; this document records how the lab produced evidence for and
against them. **Where this document and the fork's ADRs disagree, the fork's
win** — they are executable, this one is not.

## Context

The Flix compiler generates one JVM class per specialization of a polymorphic
def, struct, or enum. That class name used to end in a suffix minted from a
global counter (`$1234`), whose value depended on how many symbols were
allocated before it — so it renumbered whenever an unrelated declaration was
added, removed, or reordered.

The lab exists to check that independently of the fork, against real compiled
output rather than unit tests: the fork can assert what it believes it emits,
but only a census of `.class` files says what it actually emitted.

**The measurement that motivated the work:** across a perturbation of the
standard library and a representative source file, **96.5% of counter-derived
specialization names changed between two otherwise-equivalent builds**.

## Method

| instrument | what it answers |
|---|---|
| `src/AllConstructs.flix` | a demo exercising every id-bearing symbol kind, so a census has something of each to find |
| `scripts/class-id-report` | censuses an already-built `build/class` directory without invoking the compiler, telling counter-derived ids from hash-derived ones |
| `scripts/edit-resistance` | compiles a source once unmodified and once per perturbation, reporting what fraction of names and bytes survive each |
| `scripts/collision-stress` | generates enough specializations to make a collision likely at a given width and compiles them for real, measuring the collision policy instead of reasoning about it |
| `scripts/duplicate-decl-stress` | generates byte-identical duplicate declarations and checks each is still cited at its own line |
| `fixtures/pass`, `fixtures/fail` | one claim per file, checked by `scripts/fixtures-check` against committed `.names`/`.sym`/`.hashes`/`.expect` snapshots |

Two properties of the method are worth stating, because they are what make the
results evidence rather than assertion:

- **The census never invokes the compiler.** It reads constant pools from
  `.class` files, so it cannot inherit a belief from the code under test.
- **Snapshots are regenerated deliberately, against a stated build.** A snapshot
  diff is only informative if it is known which compiler produced it; the fork's
  `NameFormat.Version` is what distinguishes "the compiler names things
  differently now" from "this program changed".

## Measurements

### Width nesting is real, not incidental

Built and compiled at widths 4, 8 and 12:

| Class | width=4 | width=8 | width=12 |
| --- | --- | --- | --- |
| `Def$println$*` | `tsp2` | `l2f6tsp2` | `tly4l2f6tsp2` |
| `List$*$Nil` | `za9x` | `hd9oza9x` | `yamvhd9oza9x` |
| `Option$*$None` | `rq1k` | `qu22rq1k` | `hq54qu22rq1k` |
| `ToString/Def$toString$*` (×3) | `c0ht`, `inv0`, `of6y` | `g5pec0ht`, `7xeeinv0`, `2mtmof6y` | `xiybg5pec0ht`, `ts307xeeinv0`, `l88h2mtmof6y` |

Every narrower suffix is exactly the trailing substring of the wider one, at
every width tested — the modulo truncation nesting rather than an unrelated
shorter hash. Turning the width down chops characters off an existing name; it
never renumbers one.

### The collision guard fires at a narrowed width

`collision-stress` at width 2 (1,296 possible suffixes; 134 specializations
already crosses 99.9% on the birthday bound) reproduced one for real:

```
Specialization name collision on 'collisionStressProbe$si' from key
'collisionStressProbe|((((Arrow(3) Pure) Enum(M34)) Str) Str)':
'collisionStressProbe' at 'M119 -> (String -> String)' and
'collisionStressProbe' at 'M34 -> (String -> String)'.
```

Thrown as `InternalCompilerException` from `CollisionRegistry.claim` — not a
graceful compile error, and not a silent overwrite. The fork's collision policy
is what this confirms; the confirmation is that it holds at a width where
collisions are reachable, rather than only being argued at the default.

### `--Xstable-name-length 0` reaches every family

Instance-member and derived-instance ids once minted at a different call site
from the rest (`SymId.Hash(StableName.suffix(key))` directly, rather than through
`Symbol.stableOrCounterId`), which raised a question reading the call sites could
not settle: does the documented "falls back to classic incrementing ids"
behaviour reach them?

Measured rather than reasoned about. `./scripts/fixtures-build --stable-width 0`
compiles `fixtures/pass/instance-member.flix` and
`fixtures/pass/derived-instance.flix` at width 0; censusing with
`./scripts/class-id-report --stable-width 12` — 12, not 0: the width being
searched for, not the width the build used — found **5 sequential counters and 0
hash-derived ids for each fixture**.

That call-site distinction is now historical (fork commit `6a298af02`), and the
re-measurement against the newer build gives the same 5/0 result, with
`M/Def$greet$220010` and `Eq/Def$eq$220030` in place of the default-width
`M/Def$greet$4rq6fkt2d6ho` and `Eq/Def$eq$kerxy4p6hwwg`.

What changed is *where* the flag is observable. Before, it reached the rendered
class name but not the AST-level `Symbol.DefnSym.name` a def carries at
`check()` time, which stayed hash-shaped regardless — so `fixtures-check`'s own
`sourceInstanceDefIds` guard could never fire through that path. It fires now,
confirmed on the same re-measurement.

`instance-member.flix` spells its two instances `instance M.Greet[Int32]` and
`instance Greet[String]`, one qualified and one not, specifically to exercise the
resolved-trait path `6a298af02` introduced; its `M/` package prefix in the census
is that fixture's own shape, not an artefact of the flag.

## What the lab does not test

The fixtures test user-visible compilation output: generated class names, and
diagnostics emitted under compilation failure. Internal AST symbol identity —
whether duplicate instance members share a `Symbol.DefnSym` before validation,
whether duplicate derived defs share one under error recovery — is not reachable
from outside the compiler and lives in the fork's own suite
(`main/test/ca/uwaterloo/flix/language/phase/TestSourceIdentity.scala`). The
lab's `.expect` snapshots verify only that diagnostics stay graceful and
deterministic regardless of how symbols are shared internally.
