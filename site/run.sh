#!/bin/bash
set -e
PORT="${1:-8080}"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
WEBAPP_DIR="$PROJECT_DIR/target/webapp"

echo "=== Building teavm-lambda-site ==="
mvn -f "$PROJECT_DIR/pom.xml" clean process-classes -q

if [ ! -f "$WEBAPP_DIR/js/classes.js" ]; then
    echo "ERROR: Build did not produce classes.js"
    exit 1
fi

echo "Build complete."
echo ""
echo "=== Serving at http://localhost:$PORT ==="
echo "Press Ctrl+C to stop."
echo ""
python3 -m http.server "$PORT" --directory "$WEBAPP_DIR"
