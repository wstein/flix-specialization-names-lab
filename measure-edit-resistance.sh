#!/usr/bin/env bash
#
# Measures how many generated class names change when the source is perturbed.
#
# A name that survives an edit can be cached, diffed, and reused across builds; one that
# does not forces downstream work even though nothing it describes has changed. This
# reports, per kind of edit, the share of names that survive it.
#
# The measurement is only meaningful against a reproducible baseline: while two builds of
# identical source disagreed, edit sensitivity could not be separated from build-to-build
# churn. Run 0 below asserts that baseline rather than assuming it.
#
# Usage: ./measure-edit-resistance.sh [project-dir]

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="${1:-${SCRIPT_DIR}}"
SRC="${PROJECT_DIR}/src/hello.flix"
JAR="${PROJECT_DIR}/../../out/flix/assembly.dest/out.jar"

if [[ ! -f "${JAR}" ]]; then
    echo "Error: compiler jar not found at ${JAR}. Build it with './mill flix.assembly'." >&2
    exit 1
fi

WORK="$(mktemp -d)"
trap 'rm -rf "${WORK}"; cp -f "${WORK}.orig" "${SRC}" 2>/dev/null || true' EXIT
cp "${SRC}" "${WORK}.orig"

# Records the sorted class file names of a build into $1.
snapshot() {
    rm -rf "${PROJECT_DIR}/build"
    (cd "${PROJECT_DIR}" && java -jar "${JAR}" build-classes >/dev/null 2>&1)
    (cd "${PROJECT_DIR}/build/class" && find . -name '*.class' | sort) > "$1"
}

# Reports what share of the names in $1 also appear in $2.
#
# Also reports whether the edit reached code generation at all. An edit that adds no class
# and removes none produced identical output, which for a comment is the result we want and
# for an inserted lambda means the measurement did not measure anything — an unused binding
# is optimized away before it can be lifted.
report() {
    local label="$1" before="$2" after="$3"
    python3 - "$label" "$before" "$after" <<'PY'
import sys
label, before, after = sys.argv[1], sys.argv[2], sys.argv[3]
a = {l.strip() for l in open(before)}
b = {l.strip() for l in open(after)}
kept = len(a & b)
added, removed = len(b - a), len(a - b)
effect = "output unchanged" if a == b else f"+{added} -{removed} classes"
print(f"  {label:<34} {100*kept/len(a):6.2f}% survive   ({removed} of {len(a)} renamed, {effect})")
PY
}

echo "Baseline"
snapshot "${WORK}/base.txt"
snapshot "${WORK}/base2.txt"
report "rebuild, no change" "${WORK}/base.txt" "${WORK}/base2.txt"

# Each perturbation is applied to a pristine copy of the source, so they do not compound.
perturb() {
    local label="$1" script="$2"
    cp "${WORK}.orig" "${SRC}"
    python3 -c "$script" "${SRC}"
    snapshot "${WORK}/after.txt"
    report "${label}" "${WORK}/base.txt" "${WORK}/after.txt"
    cp "${WORK}.orig" "${SRC}"
}

echo
echo "Perturbations"

perturb "add a comment" '
import sys
p=sys.argv[1]; s=open(p).read()
open(p,"w").write("// an added comment\n"+s)
'

perturb "add a blank line inside a def" '
import sys
p=sys.argv[1]; s=open(p).read()
open(p,"w").write(s.replace("def boxDemo(): String = region rc {", "def boxDemo(): String = region rc {\n", 1))
'

perturb "rename a local variable" '
import sys
p=sys.argv[1]; s=open(p).read()
open(p,"w").write(s.replace("let pipeRes =", "let pipeResult =").replace("${pipeRes}", "${pipeResult}"))
'

perturb "add an unrelated def at the end" '
import sys
p=sys.argv[1]; s=open(p).read()
open(p,"w").write(s+"\ndef unrelatedAddition(x: Int32): Int32 = x + 1\n")
'

# The inserted lambda has to reach code generation, so its result is used. An unused
# binding is optimized away and would leave the output untouched, measuring nothing.
perturb "insert a lambda earlier in a def" '
import sys
p=sys.argv[1]; s=open(p).read()
old="def repeatedStdlibDemo(): Int32 = {"
new=old+"\n    let z = List.map(x -> x + 100, 1 :: Nil) |> List.length;"
s=s.replace(old,new,1)
s=s.replace("    a + b + c + d + e + f + g + h + i\n", "    a + b + c + d + e + f + g + h + i + z\n",1)
open(p,"w").write(s)
'

echo
echo "A name that survives is one a downstream cache can keep."
echo
echo "Caveat: the lambdas in this program are inlined into specializations of the library"
echo "functions they are passed to, so they are lifted out of those rather than out of the"
echo "definition that wrote them. That makes this corpus weak evidence about renumbering"
echo "within a single definition; a program with two lambdas lifted from one def would test"
echo "it directly."
