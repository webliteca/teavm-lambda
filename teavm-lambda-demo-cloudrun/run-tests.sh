#!/usr/bin/env bash
#
# Runs the full integration test suite for the Cloud Run demo.
# PostgreSQL runs in Docker (no host ports).
# The Cloud Run app runs in Docker on the same network.
#
# Prerequisites:
#   - mvn clean package -pl teavm-lambda-demo-cloudrun -am
#   - docker, curl, jq on PATH
#
# Usage:
#   ./teavm-lambda-demo-cloudrun/run-tests.sh
#
set -eo pipefail

cd "$(dirname "$0")/.."

export COMPOSE_PROJECT_NAME=teavmlambda-cloudrun-test
COMPOSE_FILE=docker-compose.cloudrun.yml
NETWORK="${COMPOSE_PROJECT_NAME}_default"
APP_CONTAINER="teavm-cloudrun-test-app"
APP_PORT=8080
PASS=0
FAIL=0

# Check prerequisites
for cmd in docker curl jq; do
    if ! command -v "$cmd" &>/dev/null; then
        echo "ERROR: '$cmd' not found on PATH"
        exit 1
    fi
done

cleanup() {
    echo
    echo "=== Tearing down ==="
    docker rm -f "$APP_CONTAINER" 2>/dev/null || true
    docker compose -f "$COMPOSE_FILE" down -v 2>/dev/null || true
}
trap cleanup EXIT

# --- Setup ---
echo "=== Starting PostgreSQL ==="
docker compose -f "$COMPOSE_FILE" up -d --wait
echo
echo "Using Docker network: $NETWORK"
echo

echo "=== Building Cloud Run app image ==="
docker build -f teavm-lambda-demo-cloudrun/docker/Dockerfile -t teavm-cloudrun-demo . -q
echo

echo "=== Starting Cloud Run app ==="
docker run -d --name "$APP_CONTAINER" \
    --network "$NETWORK" \
    -e DATABASE_URL="postgresql://demo:demo@postgres:5432/demo" \
    -e PORT="$APP_PORT" \
    teavm-cloudrun-demo
echo

# Wait for app to be ready
echo "=== Waiting for app to start ==="
for i in $(seq 1 30); do
    if docker exec "$APP_CONTAINER" sh -c "wget -q -O /dev/null http://localhost:$APP_PORT/health" 2>/dev/null; then
        echo "App is ready!"
        break
    fi
    if [ "$i" -eq 30 ]; then
        echo "ERROR: App did not start in time"
        docker logs "$APP_CONTAINER"
        exit 1
    fi
    sleep 1
done
echo

# Get the app container IP on the test network
APP_IP=$(docker inspect -f "{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}" "$APP_CONTAINER")
BASE_URL="http://${APP_IP}:${APP_PORT}"

run_test() {
    local method="$1" path="$2" expected_status="$3" body_check="$4" desc="$5" body_desc="${6:-}" request_body="${7:-}"

    local response status body
    local curl_args=(-s -w '\n%{http_code}' -X "$method")

    if [ -n "$request_body" ]; then
        curl_args+=(-H "Content-Type: application/json" -d "$request_body")
    fi

    response=$(docker exec "$APP_CONTAINER" sh -c "wget -q -O - --method=$method --header='Content-Type: application/json' ${request_body:+--body-data='$request_body'} http://localhost:$APP_PORT$path; echo; echo \$?" 2>/dev/null || echo "")

    # Use a temp container on the same network to make HTTP calls
    local full_response
    full_response=$(docker run --rm --network "$NETWORK" curlimages/curl:latest \
        -s -w '\n%{http_code}' -X "$method" \
        ${request_body:+-H "Content-Type: application/json" -d "$request_body"} \
        "${BASE_URL}${path}" 2>/dev/null || echo "")

    status=$(echo "$full_response" | tail -1)
    body=$(echo "$full_response" | sed '$d')

    # Check status
    if [ "$status" = "$expected_status" ]; then
        echo "  PASS: $desc (status $status)"
        PASS=$((PASS + 1))
    else
        echo "  FAIL: $desc - expected status $expected_status, got: $status"
        echo "        body: $body"
        FAIL=$((FAIL + 1))
    fi

    # Check body (optional)
    if [ -n "$body_desc" ]; then
        if echo "$body" | grep -q "$body_check"; then
            echo "  PASS: $body_desc"
            PASS=$((PASS + 1))
        else
            echo "  FAIL: $body_desc - expected body to contain '$body_check'"
            echo "        body: $body"
            FAIL=$((FAIL + 1))
        fi
    fi
}

echo "=== Running integration tests ==="
echo

echo "--- Health ---"
run_test GET /health 200 '"status":"ok"' \
    "GET /health returns 200" "GET /health body contains ok"

echo
echo "--- List Users ---"
run_test GET /users 200 '"name":"Alice"' \
    "GET /users returns 200" "GET /users contains Alice"

echo
echo "--- Get User ---"
run_test GET /users/1 200 '"name":"Alice"' \
    "GET /users/1 returns 200" "GET /users/1 is Alice"
run_test GET /users/999 404 '"error"' \
    "GET /users/999 returns 404" ""

echo
echo "--- Create User ---"
run_test POST /users 201 '"name":"TestUser"' \
    "POST /users returns 201" "POST /users returns name" \
    '{"name":"TestUser","email":"test@example.com"}'

echo
echo "--- Delete User ---"
run_test DELETE /users/3 204 "" \
    "DELETE /users/3 returns 204" ""

echo
echo "--- Routing ---"
run_test GET /nonexistent 404 "Not Found" \
    "GET /nonexistent returns 404" "404 body is Not Found"

# --- Results ---
echo
echo "======================================="
echo "Results: $PASS passed, $FAIL failed"
echo "======================================="

if [ "$FAIL" -gt 0 ]; then
    exit 1
fi
