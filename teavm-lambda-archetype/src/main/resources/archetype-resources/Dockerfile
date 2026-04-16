FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src src
COPY docker docker
RUN mvn clean package -P cloudrun -q

FROM node:22-slim
WORKDIR /app
COPY --from=build /app/target/cloudrun/ .
RUN npm install --production
EXPOSE 8080
CMD ["node", "server.js"]
