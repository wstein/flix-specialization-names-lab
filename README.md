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

This repository is scaffolded from
[`flix-template`](https://github.com/wstein/flix-template) and inherits its
[`flixw`](https://github.com/wstein/flixw) bootstrap — a repository-local
wrapper that fetches the pinned compiler instead of relying on whatever
`flix` happens to be installed, which matters here because the lab needs to
run two different compiler builds (counter-suffix vs. hash-suffix) against
the same source.

## What the lab measures

The workflow, carried over from the exploration on the
`flix-specialization-names-lab` branch of the compiler fork, is:

1. **`src/hello.flix`** — a demo deliberately written to touch every
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
4. Comparing that census across recompiles — with and without unrelated
   source edits — is what shows whether a naming scheme is actually stable:
   a hash-derived id should reappear unchanged; a counter-derived one
   renumbers as soon as an earlier declaration shifts.

## Quick start

```sh
./flixw run          # .\flixw.cmd run on Windows
```

The only prerequisite is a JDK, Java 21 or newer. You do not need Flix
installed: the first command downloads `flix.jar` for the version pinned in
`.flixw/lock.toml`, checks it against the SHA-256 committed alongside it, caches
it outside the repository, and runs it. Later commands reuse the cache.

```sh
./flixw check        # type-check; the fast feedback loop
./flixw test         # run every @Test function under test/
./flixw format       # reformat sources in place
./flixw validate     # the wrapper's own consistency checks; what CI runs first
./flixw doctor       # validate, plus the full picture, for bug reports
```

## What is in here

```
.
├── src/
│   └── Main.flix                 template placeholder — to be replaced by the
│                                  hello.flix demo from the lab branch
├── test/
│   └── TestMain.flix             template placeholder @Test functions
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
└── LICENSE                       Apache-2.0, with the copyright line to replace
```

`flix.toml` states a *floor* and `.flixw/lock.toml` states the *pin*. They are
allowed to differ — any pin at or above the floor satisfies it — but
`./flixw validate` fails when the pin does not, so the two cannot drift apart
unnoticed.

## What the wrapper is and is not

`flixw` never patches, forks or links against the Flix compiler. It fetches the
stock `flix.jar` by URL, verifies the digest before every use, and runs it as an
opaque process. Moving to another compiler is `./flixw pin <version>`, which
rewrites the lock; updating the wrapper itself is
`./flixw wrapper --upgrade`.

Two things are worth knowing before you adopt it. `flixw` is upstream-described
as experimental, and it is code your project executes on every build — which is
why it is committed in full and pinned by version and digest rather than curled
at run time. Read `.flixw/flixw.java` if that matters to you; it is deliberately
one file.

## Continuous integration

`.github/workflows/build-and-test.yaml` runs `validate`, `check` and `test`
through the wrapper on Linux, macOS and Windows — the Windows leg exercises
`flixw.cmd`, the others the POSIX shim. It installs a JDK and nothing else,
which is the same starting position a new contributor is in. The compiler is
restored from the runner cache, keyed on `.flixw/lock.toml`, and its digest is
re-verified whether it came from the cache or the network. Actions are pinned to
commit digests and kept current by Dependabot.

There is no formatting gate: the pinned compiler's `format` has no check-only
mode, so run `./flixw format` before you commit.

`.github/workflows/update-flix.yaml` runs weekly. Dependabot has no ecosystem
for a compiler pinned by URL and digest, so this is its counterpart: it resolves
the newest `flix/flix` release, re-pins, runs `validate`, `check` and `test`,
and opens a pull request if all three pass. It never pushes to the default
branch — the digest in a re-pinned lock is computed by the runner, and that is
the thing worth reading before merging.

`.github/workflows/docs.yaml` runs `./flixw doc` on every push to `main` and
publishes this project's own pages to GitHub Pages — for this repository, at
<https://wstein.github.io/flix-specialization-names-lab/>.

`flix doc` renders the whole standard library alongside the project and has no
option to narrow that: `--Xlib` decides what is *compiled*, and without the
library nothing compiles at all. Its `index.html` is the stdlib's `Prelude`
page. So the workflow picks out the project's pages afterwards — by which ones
carry a source link into the workspace, which no library page does — writes its
own landing page listing them, and refuses to publish at all if that finds
nothing. A link check then fails the build if anything published points at a
page that was not.

One upstream quirk is worked around there too. `flix doc` builds each `Source`
link by appending the documented file's path to the standard library's own base
URL on `flix/flix`, which for this project's files yields a 404 with the build
machine's absolute path inside it. The workflow rewrites those into permalinks
at the published commit, and fails if any filesystem path survives.

Pages has to be enabled once, under **Settings → Pages** with source
**GitHub Actions**: the default `GITHUB_TOKEN` cannot create a Pages site even
with `pages: write`. Until it is, the documentation is still built and the run
warns rather than failing, so a fresh copy of this template does not start red.

## Status

This repository was scaffolded from `flix-template` and still carries its
placeholder `src/Main.flix` and `test/TestMain.flix`. Porting over the actual
lab content — `src/hello.flix`, `class-id-report.scala`, and the `flix.toml`
metadata from the `flix-specialization-names-lab` branch of the compiler
fork — is a follow-up step, not yet done here.

The Flix and `flixw` badges read `.flixw/lock.toml` directly, so re-pinning with
`./flixw pin <version>` updates them without touching this file.

## License

Apache-2.0. See [LICENSE](LICENSE).
