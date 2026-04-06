#!/usr/bin/env bash
#
# Integration tests for the features demo (Lambda).
#
# Prerequisites:
#   - mvn clean package -pl teavm-lambda-demo-features-lambda -am
#   - docker, sam, jq on PATH
#
set -eo pipefail

cd "$(dirname "$0")/.."

export COMPOSE_PROJECT_NAME=teavmlambda-features-lambda-test
COMPOSE_FILE=docker-compose.features-lambda.yml
TEMPLATE=template.features.yaml
EVENTS_DIR=teavm-lambda-demo-features-lambda/events
NETWORK="${COMPOSE_PROJECT_NAME}_default"
PASS=0
FAIL=0

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

echo "=== Starting PostgreSQL ==="
docker compose -f "$COMPOSE_FILE" up -d --wait
echo
echo "Using Docker network: $NETWORK"
echo

invoke() {
    local event_file="$1"
    local stdout_file
    stdout_file=$(mktemp)
    SAM_CLI_TELEMETRY=0 sam local invoke FeaturesDemoFunction \
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

    status=$(echo "$response" | jq -r '.statusCode // empty')
    if [ "$status" = "$expected_status" ]; then
        echo "  PASS: $desc_status (status $status)"
        PASS=$((PASS + 1))
    else
        echo "  FAIL: $desc_status - expected status $expected_status, got: $status"
        echo "        response: $response"
        FAIL=$((FAIL + 1))
    fi

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

echo "=== Running features Lambda integration tests ==="
echo "    (each test invokes Lambda via SAM in Docker)"
echo

echo "--- Health Check ---"
run_test get-health.json 200 '"status":"UP"' \
    "GET /health returns 200" "Health check shows UP"

echo
echo "--- CORS (OPTIONS preflight) ---"
run_test options-cors.json 204 "" \
    "OPTIONS /items returns 204" ""

echo
echo "--- List Items ---"
run_test get-items.json 200 '"name":"Widget"' \
    "GET /items returns 200" "GET /items contains Widget"

echo
echo "--- Get Item ---"
run_test get-item-1.json 200 '"name":"Widget"' \
    "GET /items/1 returns 200" "GET /items/1 is Widget"

echo
echo "--- Not Found (ProblemDetail) ---"
run_test get-item-999.json 404 '"title":"Not Found"' \
    "GET /items/999 returns 404" "404 body is ProblemDetail"

echo
echo "--- Create Item ---"
run_test post-item.json 201 '"name":"TestItem"' \
    "POST /items returns 201" "POST /items returns name"

echo
echo "--- Validation (null body) ---"
run_test post-item-invalid.json 400 '"errors"' \
    "POST /items with null body returns 400" "Response contains errors"

echo
echo "--- PATCH Item ---"
run_test patch-item-1.json 200 '"quantity"' \
    "PATCH /items/1 returns 200" "PATCH returns updated item"

echo
echo "--- Delete Item ---"
run_test delete-item-2.json 204 "" \
    "DELETE /items/2 returns 204" ""

echo
echo "--- @HeaderParam ---"
response=$(invoke "$EVENTS_DIR/get-with-header.json")
headers=$(echo "$response" | jq -r '.headers // {} | to_entries[] | .key + ": " + .value' 2>/dev/null || echo "")
if echo "$headers" | grep -qi "x-request-id.*test-req-456"; then
    echo "  PASS: X-Request-Id echoed back in response headers"
    PASS=$((PASS + 1))
else
    echo "  FAIL: X-Request-Id not echoed back"
    echo "        headers: $headers"
    FAIL=$((FAIL + 1))
fi

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
