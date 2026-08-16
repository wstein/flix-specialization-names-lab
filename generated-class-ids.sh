#!/usr/bin/env bash
# Generate symbol and ID census files from existing JVM class files.
#
# This script deliberately does not compile a project or enable compiler options.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CLASS_DIR="${1:-${SCRIPT_DIR}/build/class}"
OUTPUT_DIR="${2:-${SCRIPT_DIR}}"

if [[ $# -gt 2 ]]; then
  echo "Usage: $0 [class-directory] [output-directory]" >&2
  exit 2
fi

if ! command -v python3 >/dev/null 2>&1; then
  echo "python3 is required to inspect JVM class files." >&2
  exit 1
fi

exec python3 "${SCRIPT_DIR}/trace-fresh-ids.py" "$CLASS_DIR" --output-dir "$OUTPUT_DIR"
