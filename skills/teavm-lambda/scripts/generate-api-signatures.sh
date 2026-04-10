#!/usr/bin/env bash
# Generates api-signatures.md from the teavm-lambda source tree.
# Run from the teavm-lambda repository root:
#   ./skills/teavm-lambda/scripts/generate-api-signatures.sh
#
# Output: skills/teavm-lambda/references/api-signatures.md

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
OUTPUT="${1:-$REPO_ROOT/skills/teavm-lambda/references/api-signatures.md}"

# API source directories to scan
API_DIRS=(
    "teavm-lambda-core/src/main/java/ca/weblite/teavmlambda/api"
    "teavm-lambda-db-api/src/main/java/ca/weblite/teavmlambda/api/db"
    "teavm-lambda-nosqldb/src/main/java/ca/weblite/teavmlambda/api/nosqldb"
    "teavm-lambda-objectstore/src/main/java/ca/weblite/teavmlambda/api/objectstore"
    "teavm-lambda-messagequeue/src/main/java/ca/weblite/teavmlambda/api/messagequeue"
    "teavm-lambda-adapter-war/src/main/java/ca/weblite/teavmlambda/impl/jvm/war"
    "teavm-lambda-image-api/src/main/java/ca/weblite/teavmlambda/api/image"
)

echo "# teavm-lambda API Signatures" > "$OUTPUT"
echo "" >> "$OUTPUT"
echo "> Auto-generated from source. Run \`scripts/generate-api-signatures.sh\` to regenerate." >> "$OUTPUT"
echo "> Read this file when you need exact method signatures for any teavm-lambda public API class." >> "$OUTPUT"
echo "" >> "$OUTPUT"

current_package=""
class_count=0

for dir in "${API_DIRS[@]}"; do
    full_dir="$REPO_ROOT/$dir"
    if [ ! -d "$full_dir" ]; then
        echo "Warning: $dir not found, skipping" >&2
        continue
    fi

    # Process each Java file
    find "$full_dir" -name "*.java" -type f | sort | while read -r file; do
        # Extract package
        pkg=$(grep -m1 '^package ' "$file" | sed 's/package //;s/;//')

        # Print package header if changed
        if [ "$pkg" != "$current_package" ]; then
            current_package="$pkg"
            echo "## $pkg" >> "$OUTPUT"
            echo "" >> "$OUTPUT"
        fi

        # Extract class/interface/enum name and type
        class_line=$(grep -m1 -E '^public (final |abstract )?(class|interface|enum|@interface) ' "$file" || true)
        if [ -z "$class_line" ]; then
            continue
        fi

        # Get simple class name
        class_name=$(echo "$class_line" | sed -E 's/^public (final |abstract )?(class|interface|enum|@interface) ([A-Za-z0-9_]+).*/\3/')
        class_type=$(echo "$class_line" | sed -E 's/^public (final |abstract )?(class|interface|enum|@interface) .*/\2/')

        # Check for extends/implements
        extends_info=""
        if echo "$class_line" | grep -q "extends "; then
            extends_info=$(echo "$class_line" | sed -E 's/.* extends ([A-Za-z0-9_<>, ]+)( implements.*| \{.*|$)/\1/')
        fi
        implements_info=""
        if echo "$class_line" | grep -q "implements "; then
            implements_info=$(echo "$class_line" | sed -E 's/.* implements ([A-Za-z0-9_<>, ]+)( \{.*|$)/\1/')
        fi

        header="### $class_name"
        [ -n "$extends_info" ] && header="$header (extends $extends_info)"
        [ -n "$implements_info" ] && header="$header (implements $implements_info)"
        echo "$header" >> "$OUTPUT"

        # For annotations, extract fields
        if [ "$class_type" = "@interface" ]; then
            grep -E '^\s+(String|int|long|double|boolean|Class|String\[\])' "$file" | \
                sed 's/^[[:space:]]*/- `/' | sed 's/$/`/' >> "$OUTPUT" || true
        else
            # Extract public methods and constructors
            grep -E '^\s+public ' "$file" | \
                grep -v -E '^\s+public (final |abstract )?(class|interface|enum)' | \
                sed 's/^[[:space:]]*//' | \
                sed 's/ {$//' | \
                sed 's/^/- `/' | \
                sed 's/$/`/' >> "$OUTPUT" || true
        fi

        echo "" >> "$OUTPUT"
        class_count=$((class_count + 1))
    done
done

echo "Generated $OUTPUT with signatures from ${#API_DIRS[@]} source directories" >&2
