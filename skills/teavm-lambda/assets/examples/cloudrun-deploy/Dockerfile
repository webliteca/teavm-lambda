# Build stage: compile Java to JavaScript via TeaVM
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q || true
COPY src src
COPY docker docker
RUN mvn clean package -Dexec.skip=true -q

# Runtime stage: Node.js 22 slim image
FROM node:22-slim
WORKDIR /app
COPY --from=build /app/target/cloudrun/ .
RUN npm install --production
EXPOSE 8080
CMD ["node", "server.js"]
