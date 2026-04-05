#!/usr/bin/env bash
#
# Integration tests for the Node.js sharp image operations.
# Validates the same sharp API calls that the TeaVM-compiled
# JsImageProcessor invokes at runtime.
#
# Prerequisites:
#   - Node.js 22 on PATH
#
# Usage:
#   ./teavm-lambda-image/run-tests.sh
#
set -eo pipefail

cd "$(dirname "$0")/test"

echo "=== Installing sharp for Node.js image tests ==="
npm install --production 2>&1 | tail -3
echo

node image-test.js
