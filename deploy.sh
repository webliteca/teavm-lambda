#!/usr/bin/env bash
#
# Deploys the TeaVM Lambda demo REST app to AWS.
#
# This script builds the Lambda artifact using Docker, then deploys it via
# AWS SAM CLI. It creates an API Gateway endpoint that proxies all requests
# to the Lambda function.
#
# Prerequisites:
#   - docker on PATH
#   - sam (AWS SAM CLI) on PATH
#   - AWS credentials configured (via env vars, ~/.aws/credentials, or IAM role)
#
# Usage:
#   ./deploy.sh                          # guided deploy (prompts for settings)
#   ./deploy.sh --database-url <url>     # set DATABASE_URL for the function
#   ./deploy.sh --stack-name <name>      # custom CloudFormation stack name
#   ./deploy.sh --region <region>        # AWS region (default: from AWS config)
#   ./deploy.sh --skip-build             # skip Docker build, use existing artifact
#
set -eo pipefail

cd "$(dirname "$0")"

# --- Defaults ---
STACK_NAME="teavm-lambda-demo"
DATABASE_URL=""
REGION=""
SKIP_BUILD=false
S3_BUCKET=""

# --- Parse arguments ---
while [[ $# -gt 0 ]]; do
    case "$1" in
        --database-url)  DATABASE_URL="$2"; shift 2 ;;
        --stack-name)    STACK_NAME="$2"; shift 2 ;;
        --region)        REGION="$2"; shift 2 ;;
        --skip-build)    SKIP_BUILD=true; shift ;;
        --s3-bucket)     S3_BUCKET="$2"; shift 2 ;;
        -h|--help)
            sed -n '2,/^$/s/^# \?//p' "$0"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

# --- Check prerequisites ---
for cmd in docker sam; do
    if ! command -v "$cmd" &>/dev/null; then
        echo "ERROR: '$cmd' not found on PATH"
        exit 1
    fi
done

if ! aws sts get-caller-identity &>/dev/null 2>&1; then
    echo "ERROR: AWS credentials not configured. Run 'aws configure' or set AWS_* env vars."
    exit 1
fi

REGION_ARGS=""
if [ -n "$REGION" ]; then
    REGION_ARGS="--region $REGION"
fi

# --- Build ---
if [ "$SKIP_BUILD" = false ]; then
    echo "=== Building Lambda artifact ==="
    docker build -f teavm-lambda-demo/docker/Dockerfile -t teavm-lambda-demo .

    echo "=== Extracting function.zip ==="
    CONTAINER_ID=$(docker create teavm-lambda-demo)
    docker cp "$CONTAINER_ID:/app/function.zip" ./function.zip
    docker rm "$CONTAINER_ID" >/dev/null

    # Unpack into the location SAM expects
    rm -rf teavm-lambda-demo/target/lambda
    mkdir -p teavm-lambda-demo/target/lambda
    unzip -qo function.zip -d teavm-lambda-demo/target/lambda
    rm function.zip
    echo "  Build artifacts ready in teavm-lambda-demo/target/lambda/"
    echo
fi

# Verify build artifacts exist
if [ ! -f teavm-lambda-demo/target/lambda/index.js ]; then
    echo "ERROR: Build artifacts not found in teavm-lambda-demo/target/lambda/"
    echo "       Run without --skip-build to build first."
    exit 1
fi

# --- Deploy ---
echo "=== Deploying to AWS ==="

SAM_DEPLOY_ARGS=(
    --template-file template.yaml
    --stack-name "$STACK_NAME"
    --capabilities CAPABILITY_IAM
    --no-fail-on-empty-changeset
    --resolve-s3
)

if [ -n "$REGION" ]; then
    SAM_DEPLOY_ARGS+=(--region "$REGION")
fi

if [ -n "$S3_BUCKET" ]; then
    # Replace --resolve-s3 with explicit bucket
    SAM_DEPLOY_ARGS=("${SAM_DEPLOY_ARGS[@]/--resolve-s3/}")
    SAM_DEPLOY_ARGS+=(--s3-bucket "$S3_BUCKET")
fi

if [ -n "$DATABASE_URL" ]; then
    SAM_DEPLOY_ARGS+=(--parameter-overrides "DatabaseUrl=$DATABASE_URL")
fi

sam deploy "${SAM_DEPLOY_ARGS[@]}"

# --- Output the API endpoint ---
echo
echo "=== Deployment complete ==="
API_URL=$(sam list stack-outputs \
    --stack-name "$STACK_NAME" \
    --output json \
    $REGION_ARGS 2>/dev/null \
    | python3 -c "
import sys, json
outputs = json.load(sys.stdin)
for o in outputs:
    if 'Api' in o.get('OutputKey','') or 'Endpoint' in o.get('OutputKey',''):
        print(o['OutputValue'])
        break
" 2>/dev/null || true)

if [ -n "$API_URL" ]; then
    echo "API Endpoint: $API_URL"
    echo
    echo "Try it out:"
    echo "  curl $API_URL/health"
    echo "  curl $API_URL/users"
else
    echo "Stack deployed. Check the AWS console for the API Gateway endpoint."
fi
