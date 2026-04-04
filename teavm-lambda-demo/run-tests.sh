#!/usr/bin/env bash
#
# Runs the full integration test suite. PostgreSQL runs in Docker (no host ports).
# Lambda code runs in Docker via SAM local invoke. No port conflicts possible.
#
# Prerequisites:
#   - mvn clean package -pl teavm-lambda-demo -am
#   - docker running
#   - sam CLI installed
#
# Usage:
#   ./teavm-lambda-demo/run-tests.sh
#
set -euo pipefail

cd "$(dirname "$0")/.."

COMPOSE_FILE=docker-compose.test.yml
TEMPLATE=template.test.yaml
EVENTS_DIR=teavm-lambda-demo/events
PASS=0
FAIL=0

# Detect the compose network name
NETWORK=$(docker compose -f "$COMPOSE_FILE" config --format json 2>/dev/null \
    | jq -r '.networks | to_entries[0].value.name // empty' 2>/dev/null)
if [ -z "$NETWORK" ]; then
    # Fallback: derive from directory name (compose default behavior)
    COMPOSE_PROJECT=$(basename "$(pwd)" | tr '[:upper:]' '[:lower:]' | sed 's/[^a-z0-9_-]//g')
    NETWORK="${COMPOSE_PROJECT}_default"
fi

invoke() {
    local event_file="$1"
    # sam local invoke exits non-zero after the Lambda container stops - that's normal.
    # Capture stdout (the JSON response), discard stderr (SAM logs).
    SAM_CLI_TELEMETRY=0 sam local invoke DemoFunction \
        --template "$TEMPLATE" \
        --event "$event_file" \
        --docker-network "$NETWORK" \
        2>/dev/null || true
}

assert_status() {
    local desc="$1" event="$2" expected_status="$3"
    local raw response status
    raw=$(invoke "$EVENTS_DIR/$event")
    # Extract the JSON response line (SAM may output extra text)
    response=$(echo "$raw" | grep '^{' | tail -1)
    status=$(echo "$response" | jq -r '.statusCode // empty')
    if [ "$status" = "$expected_status" ]; then
        echo "  PASS: $desc (status $status)"
        PASS=$((PASS + 1))
    else
        echo "  FAIL: $desc - expected status $expected_status, got: $status"
        echo "        response: $response"
        FAIL=$((FAIL + 1))
    fi
}

assert_body_contains() {
    local desc="$1" event="$2" expected="$3"
    local raw response body
    raw=$(invoke "$EVENTS_DIR/$event")
    response=$(echo "$raw" | grep '^{' | tail -1)
    body=$(echo "$response" | jq -r '.body // empty')
    if echo "$body" | grep -q "$expected"; then
        echo "  PASS: $desc"
        PASS=$((PASS + 1))
    else
        echo "  FAIL: $desc - expected body to contain '$expected'"
        echo "        body: $body"
        FAIL=$((FAIL + 1))
    fi
}

cleanup() {
    echo
    echo "=== Tearing down ==="
    docker compose -f "$COMPOSE_FILE" down -v 2>/dev/null || true
}
trap cleanup EXIT

# --- Setup ---
echo "=== Starting PostgreSQL ==="
docker compose -f "$COMPOSE_FILE" up -d --wait
echo

echo "=== Running integration tests ==="
echo "    (each test invokes Lambda via SAM in Docker)"
echo

# --- Tests ---
echo "--- Health ---"
assert_status     "GET /health returns 200"          get-health.json     200
assert_body_contains "GET /health body contains ok"  get-health.json     '"status":"ok"'

echo
echo "--- List Users ---"
assert_status     "GET /users returns 200"           get-users.json      200
assert_body_contains "GET /users contains Alice"     get-users.json      '"name":"Alice"'
assert_body_contains "GET /users contains Bob"       get-users.json      '"name":"Bob"'

echo
echo "--- Get User ---"
assert_status     "GET /users/1 returns 200"         get-user-1.json     200
assert_body_contains "GET /users/1 is Alice"         get-user-1.json     '"name":"Alice"'
assert_status     "GET /users/999 returns 404"       get-user-999.json   404

echo
echo "--- Create User ---"
assert_status     "POST /users returns 201"          post-user.json      201
assert_body_contains "POST /users returns name"      post-user.json      '"name":"TestUser"'

echo
echo "--- Delete User ---"
assert_status     "DELETE /users/3 returns 204"      delete-user-3.json  204

echo
echo "--- Routing ---"
assert_status     "GET /nonexistent returns 404"     get-nonexistent.json 404
assert_body_contains "404 body is Not Found"         get-nonexistent.json "Not Found"

# --- Results ---
echo
echo "======================================="
echo "Results: $PASS passed, $FAIL failed"
echo "======================================="

if [ "$FAIL" -gt 0 ]; then
    exit 1
fi
