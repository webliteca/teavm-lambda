#!/usr/bin/env bash
#
# Runs the same integration tests against both TeaVM/Node.js and JVM builds
# to verify platform parity. Both must produce identical behavior.
#
# Prerequisites:
#   - Docker, Docker Compose, SAM CLI, jq, curl on PATH
#   - JDK 21+
#
# Usage:
#   ./run-parity-tests.sh
#
# What it does:
#   1. Checks SPI parity (every SPI has both JS and JVM implementations)
#   2. Builds with -P teavm, starts SAM, runs tests
#   3. Builds with -P jvm, starts JVM server, runs same tests
#   4. Compares results
#
set -eo pipefail

cd "$(dirname "$0")"

echo "============================================"
echo " Platform Parity Test Suite"
echo "============================================"
echo

# --- Step 1: SPI Parity ---
echo "=== Step 1: SPI Parity Check ==="
./check-spi-parity.sh
SPI_RESULT=$?
echo

if [ "$SPI_RESULT" -ne 0 ]; then
    echo "SPI parity check failed — fix gaps before running integration tests."
    exit 1
fi

# --- Step 2: TeaVM Build + Test ---
echo "=== Step 2: TeaVM/Node.js Build ==="
mvn clean package -pl teavm-lambda-demo -am -P teavm -q
echo "Build complete."
echo

echo "Starting PostgreSQL..."
docker compose -f docker-compose.test.yml up -d --wait 2>/dev/null
echo

echo "Starting SAM local..."
SAM_CLI_TELEMETRY=0 sam local start-api --template template.yaml --docker-network teavmlambda-test_default &
SAM_PID=$!
sleep 5

echo "Running tests against TeaVM/Node.js..."
TEAVM_RESULT=0
./teavm-lambda-demo/test.sh http://localhost:3001 || TEAVM_RESULT=$?
kill $SAM_PID 2>/dev/null || true
echo

# --- Step 3: JVM Build + Test ---
echo "=== Step 3: JVM Build ==="
mvn clean package -pl teavm-lambda-demo -am -P jvm -q
echo "Build complete."
echo

echo "Starting JVM server..."
DATABASE_URL=postgresql://demo:demo@localhost:5432/demo \
    java -jar teavm-lambda-demo/target/teavm-lambda-demo-0.1.0-SNAPSHOT.jar &
JVM_PID=$!
sleep 3

echo "Running tests against JVM..."
JVM_RESULT=0
./teavm-lambda-demo/test.sh http://localhost:8080 || JVM_RESULT=$?
kill $JVM_PID 2>/dev/null || true
echo

# --- Cleanup ---
docker compose -f docker-compose.test.yml down -v 2>/dev/null || true

# --- Results ---
echo "============================================"
echo " Parity Results"
echo "============================================"
echo "  SPI parity:  PASS"
if [ "$TEAVM_RESULT" -eq 0 ]; then
    echo "  TeaVM tests: PASS"
else
    echo "  TeaVM tests: FAIL"
fi
if [ "$JVM_RESULT" -eq 0 ]; then
    echo "  JVM tests:   PASS"
else
    echo "  JVM tests:   FAIL"
fi
echo "============================================"

if [ "$TEAVM_RESULT" -ne 0 ] || [ "$JVM_RESULT" -ne 0 ]; then
    exit 1
fi
