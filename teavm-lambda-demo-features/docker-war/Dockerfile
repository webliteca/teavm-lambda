# Builds and runs the features demo as a WAR on Tomcat.
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
COPY teavm-lambda-adapter-war/pom.xml teavm-lambda-adapter-war/
COPY teavm-lambda-db-api/pom.xml teavm-lambda-db-api/
COPY teavm-lambda-db/pom.xml teavm-lambda-db/
COPY teavm-lambda-db-jvm/pom.xml teavm-lambda-db-jvm/
COPY teavm-lambda-logging/pom.xml teavm-lambda-logging/
COPY teavm-lambda-sentry/pom.xml teavm-lambda-sentry/
COPY teavm-lambda-nosqldb/pom.xml teavm-lambda-nosqldb/
COPY teavm-lambda-dynamodb/pom.xml teavm-lambda-dynamodb/
COPY teavm-lambda-firestore/pom.xml teavm-lambda-firestore/
COPY teavm-lambda-demo/pom.xml teavm-lambda-demo/
COPY teavm-lambda-image-api/pom.xml teavm-lambda-image-api/
COPY teavm-lambda-image/pom.xml teavm-lambda-image/
COPY teavm-lambda-image-jvm/pom.xml teavm-lambda-image-jvm/
COPY teavm-lambda-demo-cloudrun/pom.xml teavm-lambda-demo-cloudrun/
COPY teavm-lambda-demo-nosqldb/pom.xml teavm-lambda-demo-nosqldb/
COPY teavm-lambda-auth/pom.xml teavm-lambda-auth/
COPY teavm-lambda-auth-jvm/pom.xml teavm-lambda-auth-jvm/
COPY teavm-lambda-demo-auth/pom.xml teavm-lambda-demo-auth/
COPY teavm-lambda-compression/pom.xml teavm-lambda-compression/
COPY teavm-lambda-compression-jvm/pom.xml teavm-lambda-compression-jvm/
COPY teavm-lambda-demo-features/pom.xml teavm-lambda-demo-features/
COPY teavm-lambda-demo-features-lambda/pom.xml teavm-lambda-demo-features-lambda/
COPY teavm-lambda-objectstore/pom.xml teavm-lambda-objectstore/
COPY teavm-lambda-s3/pom.xml teavm-lambda-s3/
COPY teavm-lambda-s3-jvm/pom.xml teavm-lambda-s3-jvm/
COPY teavm-lambda-gcs/pom.xml teavm-lambda-gcs/
COPY teavm-lambda-gcs-jvm/pom.xml teavm-lambda-gcs-jvm/
COPY teavm-lambda-messagequeue/pom.xml teavm-lambda-messagequeue/
COPY teavm-lambda-sqs/pom.xml teavm-lambda-sqs/
COPY teavm-lambda-sqs-jvm/pom.xml teavm-lambda-sqs-jvm/
COPY teavm-lambda-pubsub/pom.xml teavm-lambda-pubsub/
COPY teavm-lambda-pubsub-jvm/pom.xml teavm-lambda-pubsub-jvm/
COPY teavm-lambda-demo-objectstore/pom.xml teavm-lambda-demo-objectstore/
COPY teavm-lambda-demo-messagequeue/pom.xml teavm-lambda-demo-messagequeue/
COPY teavm-lambda-kotlin/pom.xml teavm-lambda-kotlin/

RUN mvn dependency:go-offline -pl teavm-lambda-demo-features -am -P jvm-war -q || true

# Copy source and build
COPY teavm-lambda-core/src teavm-lambda-core/src
COPY teavm-lambda-core-jvm/src teavm-lambda-core-jvm/src
COPY teavm-lambda-processor/src teavm-lambda-processor/src
COPY teavm-lambda-adapter-war/src teavm-lambda-adapter-war/src
COPY teavm-lambda-db-api/src teavm-lambda-db-api/src
COPY teavm-lambda-db-jvm/src teavm-lambda-db-jvm/src
COPY teavm-lambda-logging/src teavm-lambda-logging/src
COPY teavm-lambda-sentry/src teavm-lambda-sentry/src
COPY teavm-lambda-compression-jvm/src teavm-lambda-compression-jvm/src
COPY teavm-lambda-demo-features/src teavm-lambda-demo-features/src

RUN mvn clean package -pl teavm-lambda-demo-features -am -P jvm-war -q

# Runtime stage — Tomcat 10.1 (Jakarta Servlet 6.0)
FROM tomcat:10.1-jdk21-temurin

# Remove default webapps
RUN rm -rf /usr/local/tomcat/webapps/*

# Deploy WAR as ROOT application
COPY --from=build /app/teavm-lambda-demo-features/target/teavm-lambda-demo-features.war \
     /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080
CMD ["catalina.sh", "run"]
