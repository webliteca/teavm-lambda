#!/usr/bin/env bash
#
# Runs the full integration test suite for the auth demo.
# PostgreSQL runs in Docker (no host ports).
# Lambda code runs in Docker via SAM local invoke.
#
# Prerequisites:
#   - mvn clean package -pl teavm-lambda-demo-auth -am
#   - docker, sam, jq on PATH
#
# Usage:
#   ./teavm-lambda-demo-auth/run-tests.sh
#
set -eo pipefail

cd "$(dirname "$0")/.."

export COMPOSE_PROJECT_NAME=teavmlambda-test-auth
COMPOSE_FILE=docker-compose.test-auth.yml
TEMPLATE=template.test-auth.yaml
EVENTS_DIR=teavm-lambda-demo-auth/events
NETWORK="${COMPOSE_PROJECT_NAME}_default"
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
echo "Using Docker network: $NETWORK"
echo

invoke() {
    local event_file="$1"
    local stdout_file
    stdout_file=$(mktemp)
    SAM_CLI_TELEMETRY=0 sam local invoke AuthDemoFunction \
        --template "$TEMPLATE" \
        --event "$event_file" \
        --docker-network "$NETWORK" \
        > "$stdout_file" 2>/dev/null || true
    local result
    result=$(grep '^{' "$stdout_file" | tail -1 || echo "")
    rm -f "$stdout_file"
    echo "$result"
}

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

echo "=== Running auth integration tests ==="
echo "    (each test invokes Lambda via SAM in Docker)"
echo

echo "--- Health (public, no auth required) ---"
run_test get-health.json 200 '"status":"ok"' \
    "GET /health returns 200 without auth" "GET /health body contains ok"

echo
echo "--- Authentication: missing token ---"
run_test get-users-no-auth.json 401 '"error":"Unauthorized"' \
    "GET /users without token returns 401" "401 body contains Unauthorized"

echo
echo "--- Authentication: bad signature ---"
run_test get-users-bad-sig.json 401 '"error":"Unauthorized"' \
    "GET /users with bad signature returns 401" "401 body contains Unauthorized"

echo
echo "--- Authentication: expired token ---"
run_test get-users-expired.json 401 '"error":"Unauthorized"' \
    "GET /users with expired token returns 401" "401 body contains Unauthorized"

echo
echo "--- Authorization: admin token (has admin+user roles) ---"
run_test get-users-admin.json 200 '"name":"Alice"' \
    "GET /users with admin token returns 200" "GET /users body contains Alice"

echo
echo "--- Authorization: user token (has user role only) ---"
run_test get-users-user.json 200 '"name":"Alice"' \
    "GET /users with user token returns 200" "GET /users body contains Alice"

echo
echo "--- Create User: admin can create (requires admin role) ---"
run_test post-user-admin.json 201 '"name":"TestUser"' \
    "POST /users with admin token returns 201" "POST /users body contains TestUser"

echo
echo "--- Create User: user role cannot create (requires admin role) ---"
run_test post-user-user-role.json 403 '"error":"Forbidden"' \
    "POST /users with user-only token returns 403" "403 body contains Forbidden"

echo
echo "--- Delete User: admin can delete ---"
run_test delete-user-3-admin.json 204 "" \
    "DELETE /users/3 with admin token returns 204" ""

echo
echo "--- SecurityContext: /users/me returns token identity ---"
run_test get-me-admin.json 200 '"subject":"alice"' \
    "GET /users/me returns 200" "GET /users/me body contains alice"

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
