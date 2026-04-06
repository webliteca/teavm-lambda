#!/usr/bin/env bash
#
# Verifies that every SPI interface has both a Node.js/TeaVM and a JVM
# implementation registered in META-INF/services.
#
# Run after `mvn compile` to check for platform parity gaps.
#
# Usage:
#   ./check-spi-parity.sh
#
set -eo pipefail

cd "$(dirname "$0")"

PASS=0
FAIL=0
WARN=0

# Maps SPI interface -> "js" or "jvm" or "both"
declare -A SPI_JS
declare -A SPI_JVM

# Known JS implementation modules (contain Node.js / TeaVM specific code)
JS_MODULES=(
    teavm-lambda-core-js
    teavm-lambda-adapter-lambda
    teavm-lambda-adapter-cloudrun
    teavm-lambda-db
    teavm-lambda-image
    teavm-lambda-s3
    teavm-lambda-gcs
    teavm-lambda-sqs
    teavm-lambda-pubsub
)

# Known JVM implementation modules
JVM_MODULES=(
    teavm-lambda-core-jvm
    teavm-lambda-adapter-lambda-jvm
    teavm-lambda-adapter-httpserver
    teavm-lambda-db-jvm
    teavm-lambda-image-jvm
    teavm-lambda-s3-jvm
    teavm-lambda-gcs-jvm
    teavm-lambda-sqs-jvm
    teavm-lambda-pubsub-jvm
)

echo "=== SPI Platform Parity Check ==="
echo

# Scan all META-INF/services files
for services_file in $(find . -path "*/META-INF/services/*" -type f 2>/dev/null | sort); do
    spi_interface=$(basename "$services_file")
    module_dir=$(echo "$services_file" | sed 's|/src/main/resources/META-INF/services/.*||' | sed 's|^\./||')

    # Classify as JS or JVM
    is_js=false
    is_jvm=false
    for m in "${JS_MODULES[@]}"; do
        if [ "$module_dir" = "$m" ]; then
            is_js=true
            break
        fi
    done
    for m in "${JVM_MODULES[@]}"; do
        if [ "$module_dir" = "$m" ]; then
            is_jvm=true
            break
        fi
    done

    if $is_js; then
        SPI_JS[$spi_interface]="yes"
    elif $is_jvm; then
        SPI_JVM[$spi_interface]="yes"
    fi
done

# Collect all unique SPI interfaces
declare -A ALL_SPIS
for spi in "${!SPI_JS[@]}"; do ALL_SPIS[$spi]=1; done
for spi in "${!SPI_JVM[@]}"; do ALL_SPIS[$spi]=1; done

# Check parity
for spi in $(echo "${!ALL_SPIS[@]}" | tr ' ' '\n' | sort); do
    has_js="${SPI_JS[$spi]:-no}"
    has_jvm="${SPI_JVM[$spi]:-no}"

    short_name=$(echo "$spi" | sed 's/.*\.//')

    if [ "$has_js" = "yes" ] && [ "$has_jvm" = "yes" ]; then
        echo "  PASS: $short_name — JS and JVM implementations found"
        PASS=$((PASS + 1))
    elif [ "$has_js" = "yes" ] && [ "$has_jvm" = "no" ]; then
        echo "  FAIL: $short_name — JS implementation found, JVM MISSING"
        FAIL=$((FAIL + 1))
    elif [ "$has_js" = "no" ] && [ "$has_jvm" = "yes" ]; then
        echo "  FAIL: $short_name — JVM implementation found, JS MISSING"
        FAIL=$((FAIL + 1))
    fi
done

# Also check for SPI interfaces defined in API modules that have NO implementations at all
echo
echo "--- Checking for unimplemented SPIs ---"
for api_module in teavm-lambda-core teavm-lambda-db-api teavm-lambda-image-api teavm-lambda-objectstore teavm-lambda-messagequeue teavm-lambda-logging teavm-lambda-sentry; do
    if [ ! -d "$api_module/src/main/java" ]; then continue; fi
    # Find interfaces that look like SPIs (end in Provider, Handler, Loader, Adapter)
    for java_file in $(find "$api_module/src/main/java" -name "*Provider.java" -o -name "*Handler.java" -o -name "*Loader.java" -o -name "*Adapter.java" 2>/dev/null); do
        # Extract the fully qualified class name
        package=$(grep "^package " "$java_file" | sed 's/package //;s/;//')
        classname=$(basename "$java_file" .java)
        fqn="$package.$classname"

        if [ -z "${ALL_SPIS[$fqn]}" ]; then
            # Check if it's actually an interface
            if grep -q "interface $classname" "$java_file"; then
                echo "  WARN: $classname ($fqn) — SPI interface with no registered implementations"
                WARN=$((WARN + 1))
            fi
        fi
    done
done

echo
echo "======================================="
echo "Results: $PASS paired, $FAIL gaps, $WARN unimplemented"
echo "======================================="

if [ "$FAIL" -gt 0 ]; then
    exit 1
fi
