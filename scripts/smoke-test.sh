#!/usr/bin/env bash
#
# ClaimTrack API smoke test - run it before AND after each modernization step.
#
#   ./scripts/smoke-test.sh                       # default http://localhost:8080/claimtrack
#   BASE_URL=http://localhost:8081/claimtrack ./scripts/smoke-test.sh
#   ./scripts/smoke-test.sh --capture baseline    # also save every response under smoke-results/baseline
#
# Spring Boot 2+ moves actuator endpoints under /actuator; set HEALTH_PATH accordingly:
#   HEALTH_PATH=/actuator/health ./scripts/smoke-test.sh
#
# Requires: bash, curl. No jq needed.
set -uo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080/claimtrack}"
HEALTH_PATH="${HEALTH_PATH:-/health}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CAPTURE_DIR=""
if [ "${1:-}" = "--capture" ]; then
  CAPTURE_DIR="$ROOT/smoke-results/${2:-run-$(date +%Y%m%d-%H%M%S)}"
  mkdir -p "$CAPTURE_DIR"
fi

PASS=0
FAIL=0
BODY=""

green() { printf '\033[32m%s\033[0m\n' "$1"; }
red()   { printf '\033[31m%s\033[0m\n' "$1"; }

# call <name> <expected-status> <user:pass|-> <method> <path> [curl args...]
call() {
  local name="$1" expected="$2" creds="$3" method="$4" path="$5"
  shift 5
  local auth=()
  [ "$creds" != "-" ] && auth=(-u "$creds")
  local tmp
  tmp="$(mktemp)"
  local status
  status="$(curl -s -o "$tmp" -w '%{http_code}' -X "$method" ${auth[@]+"${auth[@]}"} "$@" "$BASE_URL$path")"
  BODY="$(cat "$tmp")"
  rm -f "$tmp"
  if [ -n "$CAPTURE_DIR" ]; then
    printf '%s\n' "$BODY" > "$CAPTURE_DIR/$name.txt"
  fi
  if [ "$status" = "$expected" ]; then
    green "  PASS  $name ($status)"
    PASS=$((PASS + 1))
  else
    red   "  FAIL  $name - expected $expected, got $status"
    printf '        %s\n' "$(printf '%s' "$BODY" | head -c 300)"
    FAIL=$((FAIL + 1))
  fi
}

json_field() {
  # json_field <field>  - first numeric/string value of a top-level field in $BODY
  printf '%s' "$BODY" | grep -o "\"$1\":\"\{0,1\}[^,\"}]*" | head -1 | sed -E "s/\"$1\":\"?//"
}

days_ago() { date -d "-$1 days" +%d/%m/%Y 2>/dev/null || date -v-"$1"d +%d/%m/%Y; }

echo "ClaimTrack smoke test against $BASE_URL"
echo

echo "Operations"
call health        200 -                    GET  "$HEALTH_PATH"
call unauthorized  401 -                    GET  /api/claims

echo "Reference data"
call customers     200 adjuster:adjuster123 GET  /api/customers
call policies      200 adjuster:adjuster123 GET  /api/policies
call policy        200 adjuster:adjuster123 GET  /api/policies/POL-AUTO-1001
call quote         200 adjuster:adjuster123 POST /api/policies/quote \
     -H 'Content-Type: application/json' \
     -d '{"policyType":"HOME","dateOfBirth":"15/06/1980","coverageAmount":300000,"priorClaims":1}'

echo "Claim lifecycle"
call list-claims   200 adjuster:adjuster123 GET  /api/claims
call submit        201 adjuster:adjuster123 POST /api/claims \
     -H 'Content-Type: application/json' \
     -d "{\"policyNumber\":\"POL-AUTO-1002\",\"description\":\"Smoke test - cracked headlight\",\"claimAmount\":980.40,\"incidentDate\":\"$(days_ago 2)\"}"
CLAIM_ID="$(json_field id)"
echo "        claim id: ${CLAIM_ID:-<none>}"
call review        200 adjuster:adjuster123 POST "/api/claims/$CLAIM_ID/review"
call decide-403    403 adjuster:adjuster123 POST "/api/claims/$CLAIM_ID/decision" \
     -H 'Content-Type: application/json' -d '{"decision":"APPROVE"}'
call decide        200 manager:manager123   POST "/api/claims/$CLAIM_ID/decision" \
     -H 'Content-Type: application/json' -d '{"decision":"APPROVE","comment":"Smoke test approval"}'
call pay           200 manager:manager123   POST "/api/claims/$CLAIM_ID/pay"
call pay-again     409 manager:manager123   POST "/api/claims/$CLAIM_ID/pay"
call fraud         200 adjuster:adjuster123 GET  "/api/claims/$CLAIM_ID/fraud-assessment"

echo "Fraud engine (Java + scripted rules)"
call suspicious    201 adjuster:adjuster123 POST /api/claims \
     -H 'Content-Type: application/json' \
     -d "{\"policyNumber\":\"POL-TRVL-4001\",\"description\":\"Smoke test - luggage stolen\",\"claimAmount\":14000,\"incidentDate\":\"$(days_ago 1)\"}"
SUSPICIOUS_ID="$(json_field id)"
if printf '%s' "$BODY" | grep -q '"status":"ESCALATED"'; then
  green "  PASS  suspicious claim auto-escalated"; PASS=$((PASS + 1))
else
  red   "  FAIL  suspicious claim was not escalated"; FAIL=$((FAIL + 1))
fi

echo "Validation"
call bad-date      400 adjuster:adjuster123 POST /api/claims \
     -H 'Content-Type: application/json' \
     -d '{"policyNumber":"POL-AUTO-1001","description":"x","claimAmount":10,"incidentDate":"2024-01-01"}'
call lapsed        400 adjuster:adjuster123 POST /api/claims \
     -H 'Content-Type: application/json' \
     -d "{\"policyNumber\":\"POL-HOME-2002\",\"description\":\"x\",\"claimAmount\":10,\"incidentDate\":\"$(days_ago 2)\"}"
# Only Bean Validation (@DecimalMin on coverageAmount) rejects this - catches validation silently disappearing
call quote-invalid 400 adjuster:adjuster123 POST /api/policies/quote \
     -H 'Content-Type: application/json' \
     -d '{"policyType":"AUTO","dateOfBirth":"01/01/1990","coverageAmount":500,"priorClaims":0}'
call not-found     404 adjuster:adjuster123 GET  /api/claims/99999

echo "Documents (Base64 + MD5)"
DOC_DIR="$(mktemp -d)"
DOC="$DOC_DIR/police-report.txt"   # fixed name, so captures are comparable between runs
printf 'Smoke test police report for claim %s\n' "$SUSPICIOUS_ID" > "$DOC"
call upload        201 adjuster:adjuster123 POST "/api/claims/$SUSPICIOUS_ID/documents" -F "file=@$DOC;type=text/plain"
DOC_ID="$(json_field id)"
call download      200 adjuster:adjuster123 GET  "/api/claims/documents/$DOC_ID/content"
call upload-paid   400 adjuster:adjuster123 POST "/api/claims/$CLAIM_ID/documents" -F "file=@$DOC;type=text/plain"
rm -rf "$DOC_DIR"
call documents     200 adjuster:adjuster123 GET  "/api/claims/$SUSPICIOUS_ID/documents"

echo "Bulk import (StringTokenizer CSV parser)"
CSV="$("$ROOT/scripts/render-import-csv.sh")"
call import        200 adjuster:adjuster123 POST /api/claims/import -F "file=@$CSV;type=text/csv"
echo "        imported=$(json_field imported) failed=$(json_field failed)"

echo "Reports (JAXB XML + CSV)"
call report-403    403 adjuster:adjuster123 GET  /api/reports/claims-summary/xml
call report-xml    200 auditor:auditor123   GET  /api/reports/claims-summary/xml
call report-csv    200 manager:manager123   GET  /api/reports/claims-summary/csv
call audit         200 auditor:auditor123   GET  /api/audit/events

echo
echo "Passed: $PASS   Failed: $FAIL"
[ -n "$CAPTURE_DIR" ] && echo "Responses saved to $CAPTURE_DIR"
[ "$FAIL" -eq 0 ]
