#!/usr/bin/env bash
#
# Integration tests for teavm-lambda auth demo against a running server.
# Prerequisites: server running on $BASE_URL (default http://localhost:3001)
#
# Usage:
#   ./test.sh                         # uses default http://localhost:3001
#   ./test.sh http://localhost:3000   # custom URL
#
set -euo pipefail

BASE_URL="${1:-http://localhost:3001}"
PASS=0
FAIL=0

# Test tokens (HMAC-SHA256, secret: test-secret-for-demo, issuer: teavm-demo)
ADMIN_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhbGljZSIsImlzcyI6InRlYXZtLWRlbW8iLCJpYXQiOjE3MDAwMDAwMDAsImV4cCI6NDEwMjQ0NDgwMCwiZ3JvdXBzIjpbImFkbWluIiwidXNlciJdLCJuYW1lIjoiQWxpY2UiLCJlbWFpbCI6ImFsaWNlQGV4YW1wbGUuY29tIn0.Ci5FVqVwi6kpQsKr1kWLKQl0H1ABrxyAnGOhdzdUMLI"
USER_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJib2IiLCJpc3MiOiJ0ZWF2bS1kZW1vIiwiaWF0IjoxNzAwMDAwMDAwLCJleHAiOjQxMDI0NDQ4MDAsImdyb3VwcyI6WyJ1c2VyIl0sIm5hbWUiOiJCb2IiLCJlbWFpbCI6ImJvYkBleGFtcGxlLmNvbSJ9.70bXb2YwPAm82L1HyC5L6QDIW1ZEGpkD3y6WfHfpxI4"
EXPIRED_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJleHBpcmVkIiwiaXNzIjoidGVhdm0tZGVtbyIsImlhdCI6MTcwMDAwMDAwMCwiZXhwIjoxNzAwMDAwMDAxLCJncm91cHMiOlsiYWRtaW4iXX0.1MGsjM13S2PrtqS5-RuQErEKiPMJr1Y9Go8xrxcJb2E"
BAD_SIG_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhbGljZSIsImlzcyI6InRlYXZtLWRlbW8iLCJpYXQiOjE3MDAwMDAwMDAsImV4cCI6NDEwMjQ0NDgwMCwiZ3JvdXBzIjpbImFkbWluIiwidXNlciJdLCJuYW1lIjoiQWxpY2UiLCJlbWFpbCI6ImFsaWNlQGV4YW1wbGUuY29tIn0.invalidsignature"

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

echo "Testing teavm-lambda auth demo at $BASE_URL"
echo "============================================="
echo

echo "--- Health (public, no auth required) ---"
assert_status "GET /health returns 200 without auth" GET /health 200
assert_body_contains "GET /health returns ok" GET /health '"status":"ok"'

echo
echo "--- Authentication: missing token ---"
assert_status "GET /users without token returns 401" GET /users 401
assert_body_contains "401 body contains Unauthorized" GET /users '"error":"Unauthorized"'

echo
echo "--- Authentication: bad signature ---"
assert_status "GET /users with bad sig returns 401" GET /users 401 \
    -H "Authorization: Bearer $BAD_SIG_TOKEN"

echo
echo "--- Authentication: expired token ---"
assert_status "GET /users with expired token returns 401" GET /users 401 \
    -H "Authorization: Bearer $EXPIRED_TOKEN"

echo
echo "--- Authorization: admin token ---"
assert_status "GET /users with admin token returns 200" GET /users 200 \
    -H "Authorization: Bearer $ADMIN_TOKEN"
assert_body_contains "GET /users returns Alice" GET /users '"name":"Alice"' \
    -H "Authorization: Bearer $ADMIN_TOKEN"

echo
echo "--- Authorization: user token ---"
assert_status "GET /users with user token returns 200" GET /users 200 \
    -H "Authorization: Bearer $USER_TOKEN"

echo
echo "--- Create User: admin can create ---"
assert_status "POST /users with admin token returns 201" POST /users 201 \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"name":"TestUser","email":"test@example.com"}'

echo
echo "--- Create User: user role cannot create ---"
assert_status "POST /users with user token returns 403" POST /users 403 \
    -H "Authorization: Bearer $USER_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"name":"TestUser","email":"test@example.com"}'

echo
echo "--- Delete User: admin can delete ---"
LAST_ID=$(curl -s -H "Authorization: Bearer $ADMIN_TOKEN" "$BASE_URL/users" | \
    python3 -c "import sys,json; print(json.loads(sys.stdin.read())[-1]['id'])")
assert_status "DELETE /users/$LAST_ID with admin returns 204" DELETE "/users/$LAST_ID" 204 \
    -H "Authorization: Bearer $ADMIN_TOKEN"

echo
echo "--- SecurityContext: /users/me ---"
assert_status "GET /users/me returns 200" GET /users/me 200 \
    -H "Authorization: Bearer $ADMIN_TOKEN"
assert_body_contains "GET /users/me returns alice" GET /users/me '"subject":"alice"' \
    -H "Authorization: Bearer $ADMIN_TOKEN"

echo
echo "--- Routing ---"
assert_status "GET /nonexistent returns 404" GET /nonexistent 404
assert_body_contains "GET /nonexistent returns Not Found" GET /nonexistent "Not Found"

echo
echo "============================================="
echo "Results: $PASS passed, $FAIL failed"
if [ "$FAIL" -gt 0 ]; then
    exit 1
fi
