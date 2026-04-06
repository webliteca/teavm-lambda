#!/usr/bin/env bash
#
# Integration tests for the features demo (Cloud Run).
#
# Prerequisites:
#   - mvn clean package -pl teavm-lambda-demo-features -am
#   - docker, curl, jq on PATH
#
set -eo pipefail

cd "$(dirname "$0")/.."

export COMPOSE_PROJECT_NAME=teavmlambda-features-test
COMPOSE_FILE=docker-compose.features.yml
NETWORK="${COMPOSE_PROJECT_NAME}_default"
APP_CONTAINER="teavm-features-test-app"
APP_PORT=8080
PASS=0
FAIL=0

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

docker rm -f "$APP_CONTAINER" 2>/dev/null || true

echo "=== Starting PostgreSQL ==="
docker compose -f "$COMPOSE_FILE" up -d --wait
echo

echo "=== Building features demo image ==="
docker build -f teavm-lambda-demo-features/docker/Dockerfile -t teavm-features-demo . -q
echo

echo "=== Starting app ==="
docker run -d --name "$APP_CONTAINER" \
    --network "$NETWORK" \
    -e DATABASE_URL="postgresql://demo:demo@postgres:5432/demo" \
    -e PORT="$APP_PORT" \
    teavm-features-demo
echo

echo "=== Waiting for app to start ==="
for i in $(seq 1 30); do
    if docker exec "$APP_CONTAINER" node -e "require('http').get('http://localhost:$APP_PORT/health',r=>{process.exit(r.statusCode===200?0:1)}).on('error',()=>process.exit(1))" 2>/dev/null; then
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

APP_IP=$(docker inspect -f "{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}" "$APP_CONTAINER")
BASE_URL="http://${APP_IP}:${APP_PORT}"

run_test() {
    local method="$1" path="$2" expected_status="$3" body_check="$4" desc="$5" body_desc="${6:-}" request_body="${7:-}" extra_headers="${8:-}"

    local full_response
    local curl_extra=()
    if [ -n "$extra_headers" ]; then
        IFS='|' read -ra hdrs <<< "$extra_headers"
        for h in "${hdrs[@]}"; do
            curl_extra+=(-H "$h")
        done
    fi

    full_response=$(docker run --rm --network "$NETWORK" curlimages/curl:latest \
        -s -w '\n%{http_code}' -X "$method" \
        ${request_body:+-H "Content-Type: application/json" -d "$request_body"} \
        "${curl_extra[@]}" \
        "${BASE_URL}${path}" 2>/dev/null || echo "")

    local status body
    status=$(echo "$full_response" | tail -1)
    body=$(echo "$full_response" | sed '$d')

    if [ "$status" = "$expected_status" ]; then
        echo "  PASS: $desc (status $status)"
        PASS=$((PASS + 1))
    else
        echo "  FAIL: $desc - expected status $expected_status, got: $status"
        echo "        body: $body"
        FAIL=$((FAIL + 1))
    fi

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

echo "--- Health Check ---"
run_test GET /health 200 '"status":"UP"' \
    "GET /health returns 200" "Health check shows UP"
run_test GET /health 200 '"database"' \
    "GET /health has db check" "Health response includes database check"

echo
echo "--- CORS ---"
# Preflight OPTIONS should return 204 with CORS headers
full_response=$(docker run --rm --network "$NETWORK" curlimages/curl:latest \
    -s -w '\n%{http_code}' -X OPTIONS \
    -H "Origin: http://example.com" \
    -H "Access-Control-Request-Method: POST" \
    "${BASE_URL}/items" 2>/dev/null || echo "")
cors_status=$(echo "$full_response" | tail -1)
cors_body=$(echo "$full_response" | sed '$d')
if [ "$cors_status" = "204" ]; then
    echo "  PASS: OPTIONS /items returns 204"
    PASS=$((PASS + 1))
else
    echo "  FAIL: OPTIONS /items expected 204, got $cors_status"
    FAIL=$((FAIL + 1))
fi

# Check CORS headers on a normal response
cors_headers=$(docker run --rm --network "$NETWORK" curlimages/curl:latest \
    -s -D - -o /dev/null -X GET "${BASE_URL}/items" 2>/dev/null || echo "")
if echo "$cors_headers" | grep -qi "Access-Control-Allow-Origin"; then
    echo "  PASS: GET /items has CORS headers"
    PASS=$((PASS + 1))
else
    echo "  FAIL: GET /items missing CORS headers"
    FAIL=$((FAIL + 1))
fi

echo
echo "--- List Items ---"
run_test GET /items 200 '"name":"Widget"' \
    "GET /items returns 200" "GET /items contains Widget"

echo
echo "--- Get Item ---"
run_test GET /items/1 200 '"name":"Widget"' \
    "GET /items/1 returns 200" "GET /items/1 is Widget"

echo
echo "--- Not Found (structured error) ---"
run_test GET /items/999 404 '"title":"Not Found"' \
    "GET /items/999 returns 404" "404 body is ProblemDetail format"

echo
echo "--- Create Item ---"
run_test POST /items 201 '"name":"TestItem"' \
    "POST /items returns 201" "POST /items returns name" \
    '{"name":"TestItem","description":"A test item","quantity":42}'

echo
echo "--- Validation ---"
run_test POST /items 400 '"errors"' \
    "POST /items with null body returns 400" "Response contains errors" \
    ""

echo
echo "--- PATCH Item ---"
run_test PATCH /items/1 200 '"quantity"' \
    "PATCH /items/1 returns 200" "PATCH returns updated item" \
    '{"quantity":99}'

echo
echo "--- @HeaderParam ---"
# Send X-Request-Id header, expect it echoed back
header_response=$(docker run --rm --network "$NETWORK" curlimages/curl:latest \
    -s -D - -o /dev/null -X GET \
    -H "X-Request-Id: test-req-123" \
    "${BASE_URL}/items" 2>/dev/null || echo "")
if echo "$header_response" | grep -q "test-req-123"; then
    echo "  PASS: X-Request-Id echoed back in response"
    PASS=$((PASS + 1))
else
    echo "  FAIL: X-Request-Id not echoed back"
    echo "        headers: $header_response"
    FAIL=$((FAIL + 1))
fi

echo
echo "--- Delete Item ---"
run_test DELETE /items/2 204 "" \
    "DELETE /items/2 returns 204" ""

echo
echo "--- Static Files ---"
run_test GET / 200 "Features Demo" \
    "GET / returns 200" "GET / serves index.html"

echo
echo "--- OpenAPI ---"
run_test GET /openapi.json 200 '"openapi"' \
    "GET /openapi.json returns 200" "OpenAPI spec is present"

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
