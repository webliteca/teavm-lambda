#!/usr/bin/env bash
#
# Integration tests for teavm-lambda demo.
# Prerequisites: sam local start-api running on $BASE_URL (default http://localhost:3001)
#
# Usage:
#   ./test.sh                         # uses default http://localhost:3001
#   ./test.sh http://localhost:3000   # custom URL
#
set -euo pipefail

BASE_URL="${1:-http://localhost:3001}"
PASS=0
FAIL=0

assert_status() {
    local desc="$1" method="$2" path="$3" expected_status="$4"
    shift 4
    local actual_status
    actual_status=$(curl -s -o /dev/null -w "%{http_code}" -X "$method" "$@" "$BASE_URL$path")
    if [ "$actual_status" = "$expected_status" ]; then
        echo "  PASS: $desc (HTTP $actual_status)"
        PASS=$((PASS + 1))
    else
        echo "  FAIL: $desc - expected HTTP $expected_status, got $actual_status"
        FAIL=$((FAIL + 1))
    fi
}

assert_body_contains() {
    local desc="$1" method="$2" path="$3" expected="$4"
    shift 4
    local body
    body=$(curl -s -X "$method" "$@" "$BASE_URL$path")
    if echo "$body" | grep -q "$expected"; then
        echo "  PASS: $desc"
        PASS=$((PASS + 1))
    else
        echo "  FAIL: $desc - expected body to contain '$expected', got: $body"
        FAIL=$((FAIL + 1))
    fi
}

echo "Testing teavm-lambda demo at $BASE_URL"
echo "======================================="
echo

echo "--- Health ---"
assert_status "GET /health returns 200" GET /health 200
assert_body_contains "GET /health returns ok" GET /health '"status":"ok"'

echo
echo "--- List Users ---"
assert_status "GET /users returns 200" GET /users 200
assert_body_contains "GET /users returns Alice" GET /users '"name":"Alice"'
assert_body_contains "GET /users returns Bob" GET /users '"name":"Bob"'

echo
echo "--- Get User ---"
assert_status "GET /users/1 returns 200" GET /users/1 200
assert_body_contains "GET /users/1 returns Alice" GET /users/1 '"name":"Alice"'
assert_status "GET /users/999 returns 404" GET /users/999 404

echo
echo "--- Create User ---"
assert_status "POST /users returns 201" POST /users 201 \
    -H "Content-Type: application/json" -d '{"name":"TestUser","email":"test@example.com"}'
assert_body_contains "POST /users returns created user" POST /users '"name":"TestUser"' \
    -H "Content-Type: application/json" -d '{"name":"TestUser","email":"test2@example.com"}'

echo
echo "--- Delete User ---"
# Get the ID of a user we just created
LAST_ID=$(curl -s "$BASE_URL/users" | python3 -c "import sys,json; print(json.loads(sys.stdin.read())[-1]['id'])")
assert_status "DELETE /users/$LAST_ID returns 204" DELETE "/users/$LAST_ID" 204
assert_status "DELETE /users/99999 returns 404" DELETE /users/99999 404

echo
echo "--- Routing ---"
assert_status "GET /nonexistent returns 404" GET /nonexistent 404
assert_body_contains "GET /nonexistent returns Not Found" GET /nonexistent "Not Found"

echo
echo "======================================="
echo "Results: $PASS passed, $FAIL failed"
if [ "$FAIL" -gt 0 ]; then
    exit 1
fi
