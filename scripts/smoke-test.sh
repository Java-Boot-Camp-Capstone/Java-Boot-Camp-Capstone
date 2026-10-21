#!/usr/bin/env bash
# Northstar CRM smoke test - happy path AND denied paths (Lab 51: "smoke must prove 401 and 200").
#
# Usage:
#   UI_URL=http://localhost:4200 API_URL=http://localhost:8080 \
#   AGENT_PASSWORD=... ./scripts/smoke-test.sh
#
# Works against Docker Compose, a k3s port-forward, or a public demo URL.
# Tokens and passwords are never printed. Exit code != 0 if any check fails.
set -uo pipefail

UI_URL="${UI_URL:-http://localhost:4200}"
API_URL="${API_URL:-http://localhost:8080}"
AGENT_USER="${AGENT_USER:-agent}"
AGENT_PASSWORD="${AGENT_PASSWORD:?Set AGENT_PASSWORD}"
CORRELATION_ID="${CORRELATION_ID:-release-smoke-${GITHUB_RUN_ID:-local}}"

pass=0; fail=0; rows=""
check() { # name expected actual
  if [ "$2" = "$3" ]; then pass=$((pass+1)); mark="PASS"; else fail=$((fail+1)); mark="FAIL"; fi
  printf '%-4s  %-55s expected=%s got=%s\n' "$mark" "$1" "$2" "$3"
  rows="${rows}| ${mark} | $1 | $2 | $3 |"$'\n'
}
code() { curl -s -o /dev/null -w '%{http_code}' --max-time 15 "$@"; }

echo "Smoke test  UI=$UI_URL  API=$API_URL  correlation=$CORRELATION_ID"

# 1. Health
check "API readiness /actuator/health/readiness" 200 "$(code "$API_URL/actuator/health/readiness")"
check "UI serves Angular app /" 200 "$(code "$UI_URL/")"
check "UI deep link /customers (SPA fallback)" 200 "$(code "$UI_URL/customers")"

# 2. Denied paths (security negatives)
check "Anonymous GET /api/customers -> 401" 401 "$(code "$UI_URL/api/customers")"
check "Anonymous POST /api/v1/interactions -> 401" 401 "$(code -X POST -H 'Content-Type: application/json' -d '{}' "$UI_URL/api/v1/interactions")"
check "Bad password login -> 401" 401 "$(code -X POST -H 'Content-Type: application/json' -d "{\"username\":\"$AGENT_USER\",\"password\":\"definitely-wrong\"}" "$UI_URL/api/auth/login")"
check "Forged token -> 401" 401 "$(code -H 'Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.e30.Zm9yZ2Vk' "$UI_URL/api/customers")"
check "Actuator /env not exposed (denied by default)" 401 "$(code "$API_URL/actuator/env")"

# 3. Authenticated happy path (CAP-12: record interaction for Amina, CUS-1001)
json_escape() { local s=${1//\\/\\\\}; printf '%s' "${s//\"/\\\"}"; }   # no jq needed (Git Bash friendly)
login_body="{\"username\":\"$(json_escape "$AGENT_USER")\",\"password\":\"$(json_escape "$AGENT_PASSWORD")\"}"
TOKEN=$(curl -s --max-time 15 -X POST -H 'Content-Type: application/json' -d "$login_body" "$UI_URL/api/auth/login" \
  | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
[ -n "$TOKEN" ] && got=token || got=none
check "Agent login returns JWT" token "$got"
AUTH=(-H "Authorization: Bearer $TOKEN" -H "X-Correlation-Id: $CORRELATION_ID")

customers=$(curl -s --max-time 15 "${AUTH[@]}" "$UI_URL/api/customers")
check "Agent GET /api/customers -> 200" 200 "$(code "${AUTH[@]}" "$UI_URL/api/customers")"
echo "$customers" | grep -q 'CUS-1001' && got=found || got=missing
check "Customer list contains CUS-1001 (Amina Khan)" found "$got"

check "Agent POST interaction CUS-1001 -> 201" 201 "$(code -X POST "${AUTH[@]}" -H 'Content-Type: application/json' \
  -d "{\"customerId\":\"CUS-1001\",\"interactionType\":\"CALL\",\"summary\":\"Release smoke test\",\"correlationId\":\"$CORRELATION_ID\"}" \
  "$UI_URL/api/v1/interactions")"
check "Invalid interaction (blank summary) -> 400" 400 "$(code -X POST "${AUTH[@]}" -H 'Content-Type: application/json' \
  -d '{"customerId":"CUS-1001","interactionType":"CALL","summary":""}' "$UI_URL/api/v1/interactions")"
curl -s --max-time 15 "${AUTH[@]}" "$UI_URL/api/v1/interactions" | grep -q "$CORRELATION_ID" && got=found || got=missing
check "Interaction traceable by correlation id" found "$got"

echo "----"
echo "PASSED: $pass  FAILED: $fail"
if [ -n "${GITHUB_STEP_SUMMARY:-}" ]; then
  {
    echo "### Smoke test (${SMOKE_LABEL:-$UI_URL})"
    echo "| Result | Check | Expected | Got |"
    echo "|---|---|---|---|"
    printf '%s' "$rows"
    echo ""
    echo "**Passed: $pass - Failed: $fail** - correlation id \`$CORRELATION_ID\`"
  } >> "$GITHUB_STEP_SUMMARY"
fi
[ "$fail" -eq 0 ]
