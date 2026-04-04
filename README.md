# teavm-lambda

Java framework for deploying serverless functions on **AWS Lambda** and **Google Cloud Run**. Java code is compiled to JavaScript via [TeaVM](https://teavm.org) and runs on Node.js 22.

## Features

- **JAX-RS-style annotations** — `@Path`, `@GET`, `@POST`, `@PUT`, `@DELETE`, `@PathParam`, `@QueryParam`, `@Body`
- **Annotation-processed router** — routes are generated at compile time, zero reflection at runtime
- **PostgreSQL support** — async database access via the Node.js `pg` driver, exposed as blocking-style Java
- **Static file serving (Cloud Run)** — serve HTML/CSS/JS/images from `src/main/webapp/` alongside your API
- **Platform adapters** — AWS Lambda (API Gateway) and Google Cloud Run

## Quick Start

**Prerequisites:** JDK 21, Maven 3.9+, Docker, Docker Compose

### Cloud Run demo (with static files)

```bash
mvn clean package -pl teavm-lambda-demo-cloudrun -am
cd teavm-lambda-demo-cloudrun
docker compose up
```

Open [http://localhost:8081](http://localhost:8081) to see the static web UI calling the REST API.

### Lambda demo

```bash
docker compose up -d          # start PostgreSQL
mvn clean package -pl teavm-lambda-demo -am
sam local start-api --template template.yaml
# API available at http://localhost:3001
```

## Static File Serving

The Cloud Run adapter automatically serves static files from a `public/` directory at runtime. To add static files to your app:

1. **Place files in `src/main/webapp/`** — HTML, CSS, JS, images, fonts, etc.

2. **Add a Maven resource copy** to your `pom.xml`:
   ```xml
   <execution>
       <id>copy-webapp-resources</id>
       <phase>package</phase>
       <goals><goal>copy-resources</goal></goals>
       <configuration>
           <outputDirectory>${project.build.directory}/cloudrun/public</outputDirectory>
           <resources>
               <resource>
                   <directory>${project.basedir}/src/main/webapp</directory>
               </resource>
           </resources>
       </configuration>
   </execution>
   ```

3. **That's it.** Requests are resolved in this order:
   - `GET /` → `public/index.html` (directory defaults)
   - `GET /style.css` → `public/style.css` (static file)
   - `GET /users` → Java router (API route)
   - Non-GET requests always go to the Java router

Path traversal is blocked and MIME types are detected from file extensions.

## Project Structure

| Module | Purpose |
|--------|---------|
| `teavm-lambda-core` | Core annotations, Request/Response types, Router interface |
| `teavm-lambda-processor` | Annotation processor — generates `GeneratedRouter` at compile time |
| `teavm-lambda-adapter-lambda` | AWS Lambda / API Gateway adapter |
| `teavm-lambda-adapter-cloudrun` | Cloud Run HTTP server adapter (with static file serving) |
| `teavm-lambda-db` | Database layer wrapping Node.js `pg` driver via JSO |
| `teavm-lambda-demo` | Demo REST API for AWS Lambda |
| `teavm-lambda-demo-cloudrun` | Demo REST API + static web UI for Cloud Run |

## Testing

```bash
# Lambda demo integration tests
./teavm-lambda-demo/run-tests.sh

# Cloud Run demo integration tests
./teavm-lambda-demo-cloudrun/run-tests.sh
```
