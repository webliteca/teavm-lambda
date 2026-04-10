# Deployment: Google Cloud Run

> Read this when the user wants to deploy to Cloud Run, either as TeaVM/Node.js or JVM.

## TeaVM / Node.js Cloud Run (default profile)

The `teavm` profile with `teavm-lambda-adapter-cloudrun` compiles to `target/cloudrun/` containing `server.js`, `teavm-app.js`, `package.json`, and optionally a `public/` directory for static files.

### Build

```bash
mvn clean package
```

### Dockerfile (multi-stage)

```dockerfile
# Build stage
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q || true
COPY src src
RUN mvn clean package -Dexec.skip=true -q

# Runtime stage
FROM node:22-slim
WORKDIR /app
COPY --from=build /app/target/cloudrun/ .
RUN npm install --production
EXPOSE 8080
CMD ["node", "server.js"]
```

If building against the teavm-lambda multi-module repo (not released artifacts), the Dockerfile needs to copy all module POMs and sources. See `teavm-lambda-demo-cloudrun/docker/Dockerfile` for the full pattern.

### Required files in project

**docker/server.js**:
```javascript
const teavm = require('./teavm-app.js');
teavm.main([]);
```

**docker/package.json**:
```json
{
  "name": "my-cloudrun-app",
  "version": "1.0.0",
  "dependencies": {
    "pg": "^8.13.0"
  }
}
```

### Static files

Place static files in `src/main/webapp/`. Maven copies them to `target/cloudrun/public/`. The Cloud Run adapter serves them automatically:

1. `GET /` with `public/index.html` → serves index.html
2. `GET /style.css` with `public/style.css` → serves file with correct MIME type
3. Routes not matching static files → forwarded to GeneratedRouter
4. Non-GET requests always go to the router

Path traversal is blocked (`../` normalized and rejected).

### Deploy to Cloud Run

```bash
# Build and push image
docker build -t gcr.io/PROJECT_ID/my-app .
docker push gcr.io/PROJECT_ID/my-app

# Deploy
gcloud run deploy my-app \
    --image gcr.io/PROJECT_ID/my-app \
    --platform managed \
    --region us-central1 \
    --allow-unauthenticated \
    --set-env-vars DATABASE_URL=postgresql://user:pass@host:5432/db
```

### Local testing with Docker Compose

```yaml
version: '3.8'
services:
  app:
    build:
      context: .
      dockerfile: docker/Dockerfile
    ports:
      - "8080:8080"
    environment:
      DATABASE_URL: postgresql://demo:demo@postgres:5432/demo
    depends_on:
      - postgres
  postgres:
    image: postgres:16
    environment:
      POSTGRES_USER: demo
      POSTGRES_PASSWORD: demo
      POSTGRES_DB: demo
    volumes:
      - ./docker/init.sql:/docker-entrypoint-initdb.d/init.sql
```

---

## JVM Cloud Run

Use the `jvm-server` profile with `teavm-lambda-adapter-httpserver`:

```bash
mvn clean package -P jvm-server
```

### Dockerfile

```dockerfile
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src src
RUN mvn clean package -P jvm-server -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/my-app-*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
```

The JVM HttpServer adapter listens on `PORT` (default 8080) and serves static files from `./public/`.

---

## Maven POM notes

The Cloud Run (teavm) pom.xml uses `teavm-lambda-adapter-cloudrun` instead of `teavm-lambda-adapter-lambda`. The TeaVM plugin targets `${project.build.directory}/cloudrun` instead of `target/lambda/`. See `references/pom-templates.md` for the complete pom.xml.
