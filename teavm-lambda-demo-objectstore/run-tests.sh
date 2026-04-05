#!/usr/bin/env bash
#
# Runs the full integration test suite for the Object Store demo.
# MinIO runs in Docker (no host ports). A seed container creates the
# bucket and inserts test data. Lambda code runs in Docker via SAM local invoke.
#
# Prerequisites:
#   - mvn clean package -pl teavm-lambda-demo-objectstore -am
#   - docker, sam, jq on PATH
#
# Usage:
#   ./teavm-lambda-demo-objectstore/run-tests.sh
#
set -eo pipefail

cd "$(dirname "$0")/.."

export COMPOSE_PROJECT_NAME=teavmlambda-objectstore-test
COMPOSE_FILE=docker-compose.objectstore.yml
TEMPLATE=template.objectstore.yaml
EVENTS_DIR=teavm-lambda-demo-objectstore/events
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
echo "=== Starting MinIO ==="
docker compose -f "$COMPOSE_FILE" up -d minio --wait
docker compose -f "$COMPOSE_FILE" up -d minio-seed
echo

# Wait for the seed container to finish
echo "=== Waiting for MinIO seed to complete ==="
SEED_CONTAINER="${COMPOSE_PROJECT_NAME}-minio-seed-1"
for i in $(seq 1 30); do
    STATUS=$(docker inspect -f '{{.State.Status}}' "$SEED_CONTAINER" 2>/dev/null || echo "missing")
    if [ "$STATUS" = "exited" ]; then
        EXIT_CODE=$(docker inspect -f '{{.State.ExitCode}}' "$SEED_CONTAINER" 2>/dev/null || echo "1")
        if [ "$EXIT_CODE" = "0" ]; then
            echo "Seed complete!"
            break
        else
            echo "ERROR: Seed container exited with code $EXIT_CODE"
            docker logs "$SEED_CONTAINER" 2>&1 | tail -20
            exit 1
        fi
    fi
    if [ "$i" -eq 30 ]; then
        echo "ERROR: Seed container did not finish in time"
        docker logs "$SEED_CONTAINER" 2>&1 | tail -20
        exit 1
    fi
    sleep 1
done
echo
echo "Using Docker network: $NETWORK"
echo

invoke() {
    local event_file="$1"
    local stdout_file
    stdout_file=$(mktemp)
    SAM_CLI_TELEMETRY=0 sam local invoke DemoFunction \
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

echo "=== Running integration tests ==="
echo "    (each test invokes Lambda via SAM in Docker)"
echo

echo "--- Health ---"
run_test get-health.json 200 '"status":"ok"' \
    "GET /health returns 200" "GET /health body contains ok"

echo
echo "--- Upload File ---"
run_test put-file.json 201 '"status":"created"' \
    "PUT /files/test.txt returns 201" "PUT /files/test.txt body has status"

echo
echo "--- Get File ---"
run_test get-file.json 200 "Hello, World!" \
    "GET /files/test.txt returns 200" "GET /files/test.txt has content"

echo
echo "--- List Files ---"
run_test list-files.json 200 "test.txt" \
    "GET /files returns 200" "GET /files contains test.txt"

echo
echo "--- Get Missing File ---"
run_test get-missing-file.json 404 '"error"' \
    "GET /files/no-such-file.txt returns 404" ""

echo
echo "--- Delete File ---"
run_test delete-file.json 204 "" \
    "DELETE /files/test.txt returns 204" ""

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
