# Stage 1: Build with Maven + TeaVM
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy parent POM and all module POMs first (for dependency caching)
COPY pom.xml ./
COPY teavm-lambda-core/pom.xml teavm-lambda-core/
COPY teavm-lambda-processor/pom.xml teavm-lambda-processor/
COPY teavm-lambda-adapter-lambda/pom.xml teavm-lambda-adapter-lambda/
COPY teavm-lambda-db/pom.xml teavm-lambda-db/
COPY teavm-lambda-demo/pom.xml teavm-lambda-demo/

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -pl teavm-lambda-demo -am -q || true

# Copy source code
COPY teavm-lambda-core/src teavm-lambda-core/src
COPY teavm-lambda-processor/src teavm-lambda-processor/src
COPY teavm-lambda-adapter-lambda/src teavm-lambda-adapter-lambda/src
COPY teavm-lambda-db/src teavm-lambda-db/src
COPY teavm-lambda-demo/src teavm-lambda-demo/src

# Build all modules, compile demo to JS via TeaVM
RUN mvn clean package -pl teavm-lambda-demo -am -q

# Stage 2: Node.js runtime
FROM node:22-slim

WORKDIR /app

# Copy the TeaVM-compiled JS
COPY --from=build /app/teavm-lambda-demo/target/lambda/index.js ./

# Copy the local dev server
COPY teavm-lambda-demo/docker/local-server.js ./

# Install Node.js database driver
RUN npm init -y && npm install pg

EXPOSE 3000

CMD ["node", "local-server.js"]
