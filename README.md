# flix-specialization-names-lab

[![Build and Test](https://github.com/wstein/flix-specialization-names-lab/actions/workflows/build-and-test.yaml/badge.svg)](https://github.com/wstein/flix-specialization-names-lab/actions/workflows/build-and-test.yaml)
[![Flix](https://img.shields.io/badge/dynamic/toml?url=https%3A%2F%2Fraw.githubusercontent.com%2Fwstein%2Fflix-specialization-names-lab%2Fmain%2F.flixw%2Flock.toml&query=%24.compiler.version&label=flix&color=blue)](.flixw/lock.toml)
[![flixw](https://img.shields.io/badge/dynamic/toml?url=https%3A%2F%2Fraw.githubusercontent.com%2Fwstein%2Fflix-specialization-names-lab%2Fmain%2F.flixw%2Flock.toml&query=%24.wrapperVersion&label=flixw&color=blue)](https://github.com/wstein/flixw)
[![Java](https://img.shields.io/badge/java-21%2B-blue)](https://adoptium.net/temurin/releases/?version=21)
[![License](https://img.shields.io/github/license/wstein/flix-specialization-names-lab?color=blue)](LICENSE)

A lab for studying how the [Flix](https://flix.dev) compiler names the
classes it generates when it specializes polymorphic code. Historically those
names carried a sequential counter suffix (`$1234`) that renumbers on almost
any unrelated edit; a fork under study replaces it with a fixed-width,
content-addressed SHA-256 suffix (13 lowercase base-36 characters) so a
specialization's name is stable across recompiles and only changes when the
code it names actually changes.

The lab's own scripts (`edit-resistance`, `collision-stress`, `duplicate-decl-stress`) resolve a jar from `--flix-jar`/`$FLIX_JAR`, which is what actually lets the lab run two different compiler builds (counter-suffix vs. hash-suffix) against the same source.

## What the lab measures

The workflow, carried over from the exploration on the
`flix-specialization-names-lab` branch of the compiler fork, is:

1. **`src/AllConstructs.flix`** — a demo deliberately written to touch every
   id-bearing symbol kind the compiler can mint: specialized defs, lifted
   lambdas, a polymorphic struct instantiated at many types, an anonymous
   Java class, and a def called at a dozen distinct type arguments. The goal
   is a source file where nearly every generated class name is exercised at
   least once.
2. **`scripts/class-id-report`** — a `scala-cli` script that reads an already
   built `build/class` directory (it never invokes the compiler itself),
   extracts every generated symbol name from the `.class` files' constant
   pools, and censuses which ids are sequential counters versus SHA-256
   hash-derived. It writes a CSV of every symbol plus a flat list of unique
   ids, and reports counts of each id kind.
3. **`scripts/edit-resistance`** — runs the compiler in-process against a
   source file, once unmodified and once per perturbation (a comment, a
   renamed variable, an added specialization, …), and reports what fraction
   of generated class names and bytes survive each edit.
4. **`scripts/collision-stress`** — generates enough distinct specializations
   of one generic def to make a name collision likely at a given
   `--Xstable-name-length` width, compiles them for real, and reports
   whether the compiler's collision guard caught it or let it through
   silently — measuring the collision policy instead of only reasoning
   about it from the birthday bound.
5. **`scripts/duplicate-decl-stress`** — the front-end counterpart:
   generates many byte-identical duplicate top-level declarations and
   checks that every one is still cited at its own source line, so an
   incorrect program stays individually diagnosable (and stays usable by
   IDE services keyed on the same symbols) even when its declarations are
   indistinguishable by content alone.
6. **`fixtures/`** — small, focused, hand-written programs, one claim per
   file, checked by `scripts/fixtures-check`: `positive/` has one minimal
   example per id-minting family (isolated from `AllConstructs.flix`'s
   kitchen-sink shape so each is independently bisectable), `negative/`
   has duplicate-declaration patterns `duplicate-decl-stress` does not
   parametrically reach (enum, struct, trait, instance), each constructed
   so name *and* content collide, not just name.
7. Comparing that census across recompiles — with and without unrelated
   source edits — is what shows whether a naming scheme is actually stable:
   a hash-derived id should reappear unchanged; a counter-derived one
   renumbers as soon as an earlier declaration shifts.

## What is in here

```
├── src/
│   └── AllConstructs.flix        the id-bearing-symbol demo; see "What the lab measures"
├── test/
│   └── TestMain.flix             @Test functions covering AllConstructs.flix
├── scripts/
│   ├── class-id-report(.scala|.cmd)         censuses generated symbol names in build/class
│   ├── edit-resistance(.scala|.cmd)         measures name/byte survival across perturbations
│   ├── collision-stress(.scala|.cmd)        tries to trigger a stable-name collision for real
│   ├── duplicate-decl-stress(.scala|.cmd)   checks identical duplicate decls stay diagnosable
│   └── fixtures-check(.scala|.cmd)          verifies every file under fixtures/
├── fixtures/
│   ├── positive/                 one minimal, focused example per id-minting family
│   └── negative/                 duplicate-decl patterns not stress-tested elsewhere
├── docs/
│   └── adr/                      architecture decision records, e.g. the naming scheme
├── .flixw/
│   ├── flixw.java                the wrapper proper — one dependency-free Java file
│   └── lock.toml                 the exact compiler, its URL, and its SHA-256
├── .github/
│   ├── workflows/
│   │   ├── build-and-test.yaml   validate, check and test, on three platforms
│   │   ├── update-flix.yaml      weekly: re-pin the compiler, open a pull request
│   │   └── docs.yaml             build the API docs, publish them to Pages
│   └── dependabot.yml            keeps the workflows' pinned action digests current
├── flix.toml                     package metadata and the lowest Flix version accepted
├── flixw                         the POSIX shim
├── flixw.cmd                     the cmd.exe trampoline
├── AGENTS.md                     instructions for coding agents; CLAUDE.md and
│                                 .github/copilot-instructions.md point at it
└── LICENSE                       Apache-2.0
```

## Status

The demo lives at `src/AllConstructs.flix` (not the `hello.flix` name used
on that branch — it isn't a hello-world, so it's named for what it actually
is: `main`'s own banner already called it "Flix All Constructs
Demonstration"), alongside `test/TestMain.flix` and real `flix.toml`
metadata, and `scripts/class-id-report`, `scripts/edit-resistance`,
`scripts/collision-stress`, `scripts/duplicate-decl-stress`, and
`scripts/fixtures-check` are in place under `scripts/`, alongside the
`fixtures/positive/` and `fixtures/negative/` examples the last of those
checks.

## License

Apache-2.0. See [LICENSE](LICENSE).
