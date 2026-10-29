#!/usr/bin/env bash
#
# Compares two smoke-test captures, ignoring values that legitimately differ between runs
# (timestamps, generated ids, claim numbers).
#
# Usage:
#   ./scripts/compare-smoke.sh baseline modernized
#   ./scripts/compare-smoke.sh smoke-results/baseline smoke-results/modernized
#
# Exit code is 0 when the normalized responses are identical, 1 otherwise.
#
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

resolve() {
  if [ -d "$1" ]; then printf '%s' "$1"; else printf '%s' "$ROOT/smoke-results/$1"; fi
}

if [ $# -ne 2 ]; then
  echo "usage: $0 <before-capture> <after-capture>" >&2
  exit 2
fi

BEFORE="$(resolve "$1")"
AFTER="$(resolve "$2")"
for dir in "$BEFORE" "$AFTER"; do
  [ -d "$dir" ] || { echo "capture not found: $dir" >&2; exit 2; }
done

normalize() {
  sed -E \
    -e 's/[0-9]{4}-[0-9]{2}-[0-9]{2}[ T][0-9]{2}:[0-9]{2}:[0-9]{2}(\.[0-9]+)?(Z|[+-][0-9:]+)?/<timestamp>/g' \
    -e 's/"(timestamp|occurredAt)":[0-9]{10,}/"\1":<epoch>/g' \
    -e 's/CLM-[0-9]{8}-[0-9]{5}/CLM-<number>/g' \
    -e 's/"id":[0-9]+/"id":<id>/g' \
    -e 's/"free":[0-9]+/"free":<bytes>/g' \
    -e 's/claim-summary-[0-9]+\.xml/claim-summary-<n>.xml/g' \
    "$1"
}

TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
mkdir -p "$TMP/before" "$TMP/after"
for f in "$BEFORE"/*.txt; do normalize "$f" > "$TMP/before/$(basename "$f")"; done
for f in "$AFTER"/*.txt;  do normalize "$f" > "$TMP/after/$(basename "$f")";  done

if diff -ru "$TMP/before" "$TMP/after"; then
  echo "No differences (after normalizing timestamps, ids and claim numbers)."
else
  echo
  echo "Differences found. Expected ones after the Boot 2 upgrade: health.txt (actuator format) and"
  echo "unauthorized.txt (error body). Anything else is a behaviour change worth discussing."
  exit 1
fi
