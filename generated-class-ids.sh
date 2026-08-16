#!/usr/bin/env bash
#
# Census generated symbol IDs directly from JVM class-file names.
#
# This script deliberately does not compile a project or enable compiler options.
# It only reads the generated class directory, which defaults to build/class.

set -euo pipefail

CLASS_DIR="${1:-build/class}"

if [[ $# -gt 1 ]]; then
  echo "Usage: $0 [class-directory]" >&2
  exit 2
fi

if [[ ! -d "$CLASS_DIR" ]]; then
  echo "Class directory not found: $CLASS_DIR" >&2
  echo "Build classes first, or pass the class directory explicitly." >&2
  exit 1
fi

CLASS_DIR="$(cd "$CLASS_DIR" && pwd)"
TEMP_IDS="$(mktemp)"
trap 'rm -f "$TEMP_IDS"' EXIT

# IDs are the suffixes in generated JVM class names. Stable IDs use a fixed
# 13-character base-36 representation; counter IDs are decimal and at least
# three digits, avoiding ordinary names such as Tuple2.
while IFS= read -r -d '' class_file; do
  relative="${class_file#"$CLASS_DIR"/}"
  remaining="$relative"
  while [[ "$remaining" =~ \$([0-9a-z]{13}|[0-9]{3,}) ]]; do
    id="${BASH_REMATCH[1]}"
    if [[ "$id" =~ ^[0-9]{13}$ ]]; then
      # A 13-digit suffix is a stable ID, not a counter.
      printf 'stable,%s\n' "$id" >> "$TEMP_IDS"
    elif [[ "$id" =~ ^[0-9]+$ ]]; then
      printf 'counter,%s\n' "$id" >> "$TEMP_IDS"
    else
      printf 'stable,%s\n' "$id" >> "$TEMP_IDS"
    fi
    remaining="${remaining#*"$"$id}"
  done
done < <(find "$CLASS_DIR" -type f -name '*.class' -print0)

class_count="$(find "$CLASS_DIR" -type f -name '*.class' -print | wc -l | tr -d ' ')"
stable_count="$(awk -F, '$1 == "stable" { count++ } END { print count + 0 }' "$TEMP_IDS")"
counter_count="$(awk -F, '$1 == "counter" { count++ } END { print count + 0 }' "$TEMP_IDS")"

echo "Generated-class ID census"
echo "  directory: $CLASS_DIR"
echo "  class files: $class_count"
echo "  stable suffix occurrences: $stable_count"
echo "  counter suffix occurrences: $counter_count"

if [[ -s "$TEMP_IDS" ]]; then
  echo
  echo "kind,id,occurrences"
  sort "$TEMP_IDS" | uniq -c | awk '{ split($2, fields, ","); print fields[1] "," fields[2] "," $1 }'
fi
