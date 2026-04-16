#!/bin/bash
set -e
PROFILE="${1:-jvm-server}"
PORT="${2:-8080}"

echo "=== Building with profile: $PROFILE ==="
mvn clean package -P "$PROFILE" -q

case "$PROFILE" in
    jvm-server)
        echo "=== Starting JVM server on http://localhost:$PORT ==="
        PORT=$PORT java -jar target/${artifactId}-1.0.0.jar
        ;;
    lambda)
        echo "=== Starting Lambda via SAM on http://localhost:3000 ==="
        sam local start-api
        ;;
    cloudrun)
        echo "=== Starting Node.js server on http://localhost:$PORT ==="
        PORT=$PORT node target/cloudrun/server.js
        ;;
    *)
        echo "Unknown profile: $PROFILE"
        echo "Usage: ./run.sh [jvm-server|lambda|cloudrun] [port]"
        exit 1
        ;;
esac
