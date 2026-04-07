#!/usr/bin/env bash
#
# RS256 / Firebase Auth integration tests for teavm-lambda auth demo.
#
# This script generates an RSA key pair, creates RS256-signed JWTs,
# and tests the validator against a running server configured with
# JWT_ALGORITHM=RS256 and JWT_PUBLIC_KEY set to the generated public key.
#
# Prerequisites:
#   - openssl on PATH
#   - python3 on PATH
#   - Server running on $BASE_URL with RS256 configuration
#
# Usage:
#   ./test-rs256.sh                         # uses default http://localhost:3001
#   ./test-rs256.sh http://localhost:3000    # custom URL
#
set -euo pipefail

BASE_URL="${1:-http://localhost:3001}"
PASS=0
FAIL=0
TMPDIR_RS256=$(mktemp -d)
trap "rm -rf $TMPDIR_RS256" EXIT

# --- Generate RSA key pair ---
echo "Generating RSA key pair for testing..."
openssl genrsa -out "$TMPDIR_RS256/private.pem" 2048 2>/dev/null
openssl rsa -in "$TMPDIR_RS256/private.pem" -pubout -out "$TMPDIR_RS256/public.pem" 2>/dev/null

# Export public key for the server (caller should configure JWT_PUBLIC_KEY with this)
PUBLIC_KEY=$(cat "$TMPDIR_RS256/public.pem")
echo "Public key (set JWT_PUBLIC_KEY to this value):"
echo "$PUBLIC_KEY"
echo

# --- Helper: create RS256 JWT ---
create_rs256_token() {
    local payload_json="$1"
    local kid="${2:-test-key-1}"
    python3 - "$TMPDIR_RS256/private.pem" "$payload_json" "$kid" <<'PYEOF'
import sys, json, base64, subprocess, struct

private_key_path = sys.argv[1]
payload_json = sys.argv[2]
kid = sys.argv[3]

def b64url(data):
    if isinstance(data, str):
        data = data.encode('utf-8')
    return base64.urlsafe_b64encode(data).rstrip(b'=').decode('ascii')

# Header
header = json.dumps({"alg": "RS256", "typ": "JWT", "kid": kid}, separators=(',', ':'))
header_b64 = b64url(header)

# Payload
payload_b64 = b64url(payload_json)

# Signing input
signing_input = f"{header_b64}.{payload_b64}"

# Sign with openssl
proc = subprocess.run(
    ["openssl", "dgst", "-sha256", "-sign", private_key_path],
    input=signing_input.encode('utf-8'),
    capture_output=True
)
signature = b64url(proc.stdout)

print(f"{signing_input}.{signature}")
PYEOF
}

# --- Generate test tokens ---
echo "Generating RS256 test tokens..."

RS256_ADMIN_TOKEN=$(create_rs256_token '{
    "sub": "alice-firebase",
    "iss": "test-rs256-issuer",
    "iat": 1700000000,
    "exp": 4102444800,
    "groups": ["admin", "user"],
    "name": "Alice Firebase",
    "email": "alice@firebase.example.com"
}')

RS256_USER_TOKEN=$(create_rs256_token '{
    "sub": "bob-firebase",
    "iss": "test-rs256-issuer",
    "iat": 1700000000,
    "exp": 4102444800,
    "groups": ["user"],
    "name": "Bob Firebase",
    "email": "bob@firebase.example.com"
}')

RS256_EXPIRED_TOKEN=$(create_rs256_token '{
    "sub": "expired-user",
    "iss": "test-rs256-issuer",
    "iat": 1700000000,
    "exp": 1700000001,
    "groups": ["user"]
}')

RS256_BAD_SIG_TOKEN="${RS256_ADMIN_TOKEN%.*}.invalidsignature"

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

echo
echo "Testing teavm-lambda RS256 auth at $BASE_URL"
echo "============================================="
echo

echo "--- Health (public, no auth required) ---"
assert_status "GET /health returns 200 without auth" GET /health 200

echo
echo "--- RS256 Authentication: missing token ---"
assert_status "GET /users without token returns 401" GET /users 401

echo
echo "--- RS256 Authentication: bad signature ---"
assert_status "GET /users with bad RS256 sig returns 401" GET /users 401 \
    -H "Authorization: Bearer $RS256_BAD_SIG_TOKEN"

echo
echo "--- RS256 Authentication: expired token ---"
assert_status "GET /users with expired RS256 token returns 401" GET /users 401 \
    -H "Authorization: Bearer $RS256_EXPIRED_TOKEN"

echo
echo "--- RS256 Authorization: admin token ---"
assert_status "GET /users with RS256 admin token returns 200" GET /users 200 \
    -H "Authorization: Bearer $RS256_ADMIN_TOKEN"
assert_body_contains "GET /users with RS256 admin returns Alice" GET /users '"name":"Alice"' \
    -H "Authorization: Bearer $RS256_ADMIN_TOKEN"

echo
echo "--- RS256 Authorization: user token ---"
assert_status "GET /users with RS256 user token returns 200" GET /users 200 \
    -H "Authorization: Bearer $RS256_USER_TOKEN"

echo
echo "--- RS256 Create User: admin can create ---"
assert_status "POST /users with RS256 admin token returns 201" POST /users 201 \
    -H "Authorization: Bearer $RS256_ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"name":"RS256User","email":"rs256@example.com"}'

echo
echo "--- RS256 Create User: user role cannot create ---"
assert_status "POST /users with RS256 user token returns 403" POST /users 403 \
    -H "Authorization: Bearer $RS256_USER_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"name":"RS256User","email":"rs256@example.com"}'

echo
echo "--- RS256 SecurityContext: /users/me ---"
assert_status "GET /users/me with RS256 admin returns 200" GET /users/me 200 \
    -H "Authorization: Bearer $RS256_ADMIN_TOKEN"
assert_body_contains "GET /users/me returns alice-firebase" GET /users/me '"subject":"alice-firebase"' \
    -H "Authorization: Bearer $RS256_ADMIN_TOKEN"

echo
echo "============================================="
echo "Results: $PASS passed, $FAIL failed"
if [ "$FAIL" -gt 0 ]; then
    exit 1
fi
