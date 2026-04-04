#!/usr/bin/env bash
set -e

BASE_URL="${1:-http://localhost:3000}"

echo "=== Testing teavm-lambda demo at $BASE_URL ==="
echo

echo "--- GET /health ---"
curl -s "$BASE_URL/health" | jq .
echo

echo "--- GET /users (list seeded users) ---"
curl -s "$BASE_URL/users" | jq .
echo

echo "--- POST /users (create user) ---"
curl -s -X POST "$BASE_URL/users" \
  -H "Content-Type: application/json" \
  -d '{"name":"Charlie","email":"charlie@example.com"}' | jq .
echo

echo "--- GET /users/1 (get user by id) ---"
curl -s "$BASE_URL/users/1" | jq .
echo

echo "--- GET /users/999 (not found) ---"
curl -s -w "\nHTTP Status: %{http_code}\n" "$BASE_URL/users/999"
echo

echo "--- DELETE /users/3 (delete created user) ---"
curl -s -X DELETE -w "\nHTTP Status: %{http_code}\n" "$BASE_URL/users/3"
echo

echo "--- GET /users (verify final state) ---"
curl -s "$BASE_URL/users" | jq .
echo

echo "=== All tests complete ==="
