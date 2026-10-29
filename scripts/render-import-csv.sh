#!/usr/bin/env bash
#
# Renders sample-data/claims-import.template.csv into a CSV with real dates.
# Incident dates are relative to today, so the import demo never goes stale.
#
# Usage: scripts/render-import-csv.sh [output-file]   (default: target/claims-import.csv)
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TEMPLATE="$ROOT/sample-data/claims-import.template.csv"
OUT="${1:-$ROOT/target/claims-import.csv}"

days_ago() {
  # GNU date (Linux) first, then BSD date (macOS)
  date -d "-$1 days" +%d/%m/%Y 2>/dev/null || date -v-"$1"d +%d/%m/%Y
}

mkdir -p "$(dirname "$OUT")"
: > "$OUT"
while IFS= read -r line || [ -n "$line" ]; do
  while [[ "$line" =~ \{\{DAYS_AGO:([0-9]+)\}\} ]]; do
    n="${BASH_REMATCH[1]}"
    line="${line//\{\{DAYS_AGO:$n\}\}/$(days_ago "$n")}"
  done
  printf '%s\n' "$line" >> "$OUT"
done < "$TEMPLATE"

echo "$OUT"
