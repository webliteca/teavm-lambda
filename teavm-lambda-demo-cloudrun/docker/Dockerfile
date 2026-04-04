# Builds and runs the TeaVM-compiled Cloud Run application.
#
# Usage:
#   docker build -f teavm-lambda-demo-cloudrun/docker/Dockerfile -t teavm-cloudrun-demo .
#   docker run -p 8080:8080 -e DATABASE_URL=postgresql://demo:demo@host.docker.internal:5432/demo teavm-cloudrun-demo
#
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy POMs first for dependency caching
COPY pom.xml ./
COPY teavm-lambda-core/pom.xml teavm-lambda-core/
COPY teavm-lambda-processor/pom.xml teavm-lambda-processor/
COPY teavm-lambda-adapter-lambda/pom.xml teavm-lambda-adapter-lambda/
COPY teavm-lambda-adapter-cloudrun/pom.xml teavm-lambda-adapter-cloudrun/
COPY teavm-lambda-db/pom.xml teavm-lambda-db/
COPY teavm-lambda-demo/pom.xml teavm-lambda-demo/
COPY teavm-lambda-demo-cloudrun/pom.xml teavm-lambda-demo-cloudrun/

RUN mvn dependency:go-offline -pl teavm-lambda-demo-cloudrun -am -q || true

# Copy source and build
COPY teavm-lambda-core/src teavm-lambda-core/src
COPY teavm-lambda-processor/src teavm-lambda-processor/src
COPY teavm-lambda-adapter-lambda/src teavm-lambda-adapter-lambda/src
COPY teavm-lambda-adapter-cloudrun/src teavm-lambda-adapter-cloudrun/src
COPY teavm-lambda-db/src teavm-lambda-db/src
COPY teavm-lambda-demo/src teavm-lambda-demo/src
COPY teavm-lambda-demo/docker/package.json teavm-lambda-demo/docker/
COPY teavm-lambda-demo/docker/bootstrap.js teavm-lambda-demo/docker/
COPY teavm-lambda-demo-cloudrun/src teavm-lambda-demo-cloudrun/src
COPY teavm-lambda-demo-cloudrun/docker/package.json teavm-lambda-demo-cloudrun/docker/
COPY teavm-lambda-demo-cloudrun/docker/server.js teavm-lambda-demo-cloudrun/docker/

RUN mvn clean package -pl teavm-lambda-demo-cloudrun -am -q

# Runtime stage
FROM node:22-slim

WORKDIR /app
COPY --from=build /app/teavm-lambda-demo-cloudrun/target/cloudrun/ .

EXPOSE 8080
CMD ["node", "server.js"]
