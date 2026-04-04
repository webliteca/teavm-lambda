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
COPY teavm-lambda-processor/pom.xml teavm-lambda-processor/
COPY teavm-lambda-adapter-lambda/pom.xml teavm-lambda-adapter-lambda/
COPY teavm-lambda-db/pom.xml teavm-lambda-db/
COPY teavm-lambda-demo/pom.xml teavm-lambda-demo/

RUN mvn dependency:go-offline -pl teavm-lambda-demo -am -q || true

# Copy source and build
COPY teavm-lambda-core/src teavm-lambda-core/src
COPY teavm-lambda-processor/src teavm-lambda-processor/src
COPY teavm-lambda-adapter-lambda/src teavm-lambda-adapter-lambda/src
COPY teavm-lambda-db/src teavm-lambda-db/src
COPY teavm-lambda-demo/src teavm-lambda-demo/src
COPY teavm-lambda-demo/docker/package.json teavm-lambda-demo/docker/
COPY teavm-lambda-demo/docker/bootstrap.js teavm-lambda-demo/docker/

RUN mvn clean package -pl teavm-lambda-demo -am -q

# Package as a zip for Lambda deployment
RUN apt-get update && apt-get install -y zip \
    && cd teavm-lambda-demo/target/lambda \
    && zip -r /app/function.zip .
