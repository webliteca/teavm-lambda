#!/usr/bin/env bash
#
# Integration tests for the JVM image processor.
#
# Prerequisites:
#   - mvn clean package -pl teavm-lambda-image-jvm -am
#
# Usage:
#   ./teavm-lambda-image-jvm/run-tests.sh
#
set -eo pipefail

cd "$(dirname "$0")/.."

echo "=== Compiling and running JVM image integration tests ==="
echo

# Compile the test class against the module JARs
API_JAR=$(find teavm-lambda-image-api/target -name "teavm-lambda-image-api-*.jar" ! -name "*-sources*" | head -1)
JVM_JAR=$(find teavm-lambda-image-jvm/target -name "teavm-lambda-image-jvm-*.jar" ! -name "*-sources*" | head -1)

if [ -z "$API_JAR" ] || [ -z "$JVM_JAR" ]; then
    echo "ERROR: Module JARs not found. Run 'mvn clean package -pl teavm-lambda-image-jvm -am' first."
    exit 1
fi

# Find webp-imageio dependency JAR from Maven local repo
WEBP_JAR=$(find ~/.m2/repository/org/sejda/imageio/webp-imageio -name "webp-imageio-*.jar" ! -name "*-sources*" ! -name "*-javadoc*" 2>/dev/null | head -1)
CLASSPATH="$API_JAR:$JVM_JAR"
if [ -n "$WEBP_JAR" ]; then
    CLASSPATH="$CLASSPATH:$WEBP_JAR"
fi

TEST_SRC=teavm-lambda-image-jvm/src/test/java/ca/weblite/teavmlambda/impl/jvm/image/ImageIntegrationTest.java
TEST_OUT=$(mktemp -d)
trap "rm -rf $TEST_OUT" EXIT

javac -cp "$CLASSPATH" -d "$TEST_OUT" "$TEST_SRC"

echo
java -Djava.awt.headless=true -cp "$CLASSPATH:$TEST_OUT" \
    ca.weblite.teavmlambda.impl.jvm.image.ImageIntegrationTest
