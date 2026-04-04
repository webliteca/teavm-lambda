#!/usr/bin/env bash
#
# Runs the full integration test suite. PostgreSQL runs in Docker (no host ports).
# Lambda code runs in Docker via SAM local invoke. No port conflicts possible.
#
# Prerequisites:
#   - mvn clean package -pl teavm-lambda-demo -am
#   - docker, sam, jq on PATH
#
# Usage:
#   ./teavm-lambda-demo/run-tests.sh
#
set -eo pipefail

cd "$(dirname "$0")/.."

COMPOSE_FILE=docker-compose.test.yml
TEMPLATE=template.test.yaml
EVENTS_DIR=teavm-lambda-demo/events
PASS=0
FAIL=0

# Check prerequisites
for cmd in sam docker jq; do
    if ! command -v "$cmd" &>/dev/null; then
        echo "ERROR: '$cmd' not found on PATH"
        exit 1
    fi
done

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

# Detect the compose network from the postgres container we just started
NETWORK=$(docker inspect "$(docker compose -f "$COMPOSE_FILE" ps -q postgres)" \
    --format '{{range $net, $conf := .NetworkSettings.Networks}}{{$net}}{{end}}' 2>/dev/null)

if [ -z "$NETWORK" ]; then
    echo "ERROR: Could not detect Docker compose network"
    exit 1
fi
echo "Using Docker network: $NETWORK"
echo

invoke() {
    local event_file="$1"
    local stdout_file
    stdout_file=$(mktemp)
    # SAM outputs the Lambda response JSON on stdout, logs on stderr.
    # SAM exits non-zero after invocation - that's normal.
    SAM_CLI_TELEMETRY=0 sam local invoke DemoFunction \
        --template "$TEMPLATE" \
        --event "$event_file" \
        --docker-network "$NETWORK" \
        > "$stdout_file" 2>/dev/null || true
    # The response JSON is the last line of stdout that looks like JSON
    local result
    result=$(grep '^{' "$stdout_file" | tail -1 || echo "")
    rm -f "$stdout_file"
    echo "$result"
}

# Each test case invokes SAM once and checks both status and body from the same response.
run_test() {
    local event="$1" expected_status="$2" body_check="$3"
    shift 3
    local desc_status="$1" desc_body="${2:-}"
    local response status body

    response=$(invoke "$EVENTS_DIR/$event")

    if [ -z "$response" ]; then
        echo "  FAIL: $desc_status - no response from Lambda"
        FAIL=$((FAIL + 1))
        if [ -n "$desc_body" ]; then
            echo "  FAIL: $desc_body - no response from Lambda"
            FAIL=$((FAIL + 1))
        fi
        return
    fi

    # Check status
    status=$(echo "$response" | jq -r '.statusCode // empty')
    if [ "$status" = "$expected_status" ]; then
        echo "  PASS: $desc_status (status $status)"
        PASS=$((PASS + 1))
    else
        echo "  FAIL: $desc_status - expected status $expected_status, got: $status"
        echo "        response: $response"
        FAIL=$((FAIL + 1))
    fi

    # Check body (optional)
    if [ -n "$desc_body" ]; then
        body=$(echo "$response" | jq -r '.body // empty')
        if echo "$body" | grep -q "$body_check"; then
            echo "  PASS: $desc_body"
            PASS=$((PASS + 1))
        else
            echo "  FAIL: $desc_body - expected body to contain '$body_check'"
            echo "        body: $body"
            FAIL=$((FAIL + 1))
        fi
    fi
}

echo "=== Running integration tests ==="
echo "    (each test invokes Lambda via SAM in Docker)"
echo

echo "--- Health ---"
run_test get-health.json 200 '"status":"ok"' \
    "GET /health returns 200" "GET /health body contains ok"

echo
echo "--- List Users ---"
run_test get-users.json 200 '"name":"Alice"' \
    "GET /users returns 200" "GET /users contains Alice"

echo
echo "--- Get User ---"
run_test get-user-1.json 200 '"name":"Alice"' \
    "GET /users/1 returns 200" "GET /users/1 is Alice"
run_test get-user-999.json 404 '"error"' \
    "GET /users/999 returns 404" ""

echo
echo "--- Create User ---"
run_test post-user.json 201 '"name":"TestUser"' \
    "POST /users returns 201" "POST /users returns name"

echo
echo "--- Delete User ---"
run_test delete-user-3.json 204 "" \
    "DELETE /users/3 returns 204" ""

echo
echo "--- Routing ---"
run_test get-nonexistent.json 404 "Not Found" \
    "GET /nonexistent returns 404" "404 body is Not Found"

# --- Results ---
echo
echo "======================================="
echo "Results: $PASS passed, $FAIL failed"
echo "======================================="

if [ "$FAIL" -gt 0 ]; then
    exit 1
fi
