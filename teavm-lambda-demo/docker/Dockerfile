# Builds the TeaVM-compiled Lambda function artifact.
# Output: /app/function.zip ready for Lambda deployment.
#
# Usage:
#   docker build -f teavm-lambda-demo/docker/Dockerfile -t teavm-lambda-demo .
#   docker cp $(docker create teavm-lambda-demo):/app/function.zip .
#
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy POMs first for dependency caching
COPY pom.xml ./
COPY teavm-lambda-core/pom.xml teavm-lambda-core/
COPY teavm-lambda-core-js/pom.xml teavm-lambda-core-js/
COPY teavm-lambda-core-jvm/pom.xml teavm-lambda-core-jvm/
COPY teavm-lambda-processor/pom.xml teavm-lambda-processor/
COPY teavm-lambda-adapter-lambda/pom.xml teavm-lambda-adapter-lambda/
COPY teavm-lambda-adapter-lambda-jvm/pom.xml teavm-lambda-adapter-lambda-jvm/
COPY teavm-lambda-adapter-cloudrun/pom.xml teavm-lambda-adapter-cloudrun/
COPY teavm-lambda-adapter-httpserver/pom.xml teavm-lambda-adapter-httpserver/
COPY teavm-lambda-db-api/pom.xml teavm-lambda-db-api/
COPY teavm-lambda-db/pom.xml teavm-lambda-db/
COPY teavm-lambda-db-jvm/pom.xml teavm-lambda-db-jvm/
COPY teavm-lambda-logging/pom.xml teavm-lambda-logging/
COPY teavm-lambda-sentry/pom.xml teavm-lambda-sentry/
COPY teavm-lambda-nosqldb/pom.xml teavm-lambda-nosqldb/
COPY teavm-lambda-dynamodb/pom.xml teavm-lambda-dynamodb/
COPY teavm-lambda-firestore/pom.xml teavm-lambda-firestore/
COPY teavm-lambda-image-api/pom.xml teavm-lambda-image-api/
COPY teavm-lambda-image/pom.xml teavm-lambda-image/
COPY teavm-lambda-image-jvm/pom.xml teavm-lambda-image-jvm/
COPY teavm-lambda-demo/pom.xml teavm-lambda-demo/
COPY teavm-lambda-demo-cloudrun/pom.xml teavm-lambda-demo-cloudrun/
COPY teavm-lambda-demo-nosqldb/pom.xml teavm-lambda-demo-nosqldb/
COPY teavm-lambda-auth/pom.xml teavm-lambda-auth/
COPY teavm-lambda-auth-jvm/pom.xml teavm-lambda-auth-jvm/
COPY teavm-lambda-demo-auth/pom.xml teavm-lambda-demo-auth/

RUN mvn dependency:go-offline -pl teavm-lambda-demo -am -q || true

# Copy source and build
COPY teavm-lambda-core/src teavm-lambda-core/src
COPY teavm-lambda-core-js/src teavm-lambda-core-js/src
COPY teavm-lambda-processor/src teavm-lambda-processor/src
COPY teavm-lambda-adapter-lambda/src teavm-lambda-adapter-lambda/src
COPY teavm-lambda-db-api/src teavm-lambda-db-api/src
COPY teavm-lambda-db/src teavm-lambda-db/src
COPY teavm-lambda-logging/src teavm-lambda-logging/src
COPY teavm-lambda-sentry/src teavm-lambda-sentry/src
COPY teavm-lambda-demo/src teavm-lambda-demo/src
COPY teavm-lambda-demo/docker/package.json teavm-lambda-demo/docker/
COPY teavm-lambda-demo/docker/bootstrap.js teavm-lambda-demo/docker/

RUN mvn clean package -pl teavm-lambda-demo -am -q

# Package as a zip for Lambda deployment
RUN apt-get update && apt-get install -y zip \
    && cd teavm-lambda-demo/target/lambda \
    && zip -r /app/function.zip .
