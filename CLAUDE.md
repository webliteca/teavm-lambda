# CLAUDE.md

## Project Overview

TeaVM Lambda is a Java framework for deploying serverless functions on AWS Lambda and Google Cloud Run. Java code is compiled to JavaScript via TeaVM and runs on Node.js 22. It provides annotation-based HTTP routing (JAX-RS style), database connectivity via Node.js pg driver, and adapters for each platform.

**Version:** 0.1.0-SNAPSHOT  
**Package:** `io.teavmlambda`

## Build & Run

**Prerequisites:** JDK 21, Maven 3.9+, Docker, Docker Compose, AWS SAM CLI, Node.js 22

```bash
# Build everything
mvn clean package

# Build a specific module and its dependencies
mvn clean package -pl teavm-lambda-demo -am

# Local dev (Lambda demo): start PostgreSQL, then SAM
docker compose up -d
sam local start-api --template template.yaml
```

## Cloud Run Demo (with static files)

```bash
cd teavm-lambda-demo-cloudrun
docker compose up
# Open http://localhost:8081
```

This starts PostgreSQL + the app. The demo serves a static web UI from `src/main/webapp/` alongside the REST API.

## Test

There are no unit tests. Testing is done via integration test scripts that use Docker Compose + SAM CLI.

```bash
# Full automated integration tests (Lambda demo)
mvn clean package -pl teavm-lambda-demo -am
./teavm-lambda-demo/run-tests.sh

# Manual testing against running SAM API
sam local start-api --template template.yaml
./teavm-lambda-demo/test.sh http://localhost:3001

# Cloud Run demo tests
./teavm-lambda-demo-cloudrun/run-tests.sh
```

Test database: PostgreSQL 16, credentials `demo/demo`, schema in `docker/init.sql`.

## Project Structure

Multi-module Maven project:

| Module | Purpose |
|--------|---------|
| `teavm-lambda-core` | Core annotations (@Path, @GET, @POST, etc.), Request/Response types, Router interface |
| `teavm-lambda-processor` | JSR 269 annotation processor — generates `GeneratedRouter` at compile time |
| `teavm-lambda-adapter-lambda` | AWS Lambda / API Gateway adapter (JSO interop) |
| `teavm-lambda-adapter-cloudrun` | Google Cloud Run HTTP server adapter |
| `teavm-lambda-db` | Database layer wrapping Node.js pg driver via JSO |
| `teavm-lambda-demo` | Demo REST API for AWS Lambda |
| `teavm-lambda-demo-cloudrun` | Demo REST API + static web UI for Cloud Run |

## Key Dependencies

- **TeaVM 0.10.2** — Java-to-JavaScript compiler (classlib, jso, jso-apis, maven-plugin)
- **Node.js pg ^8.13.0** — PostgreSQL client (runtime, accessed via JSO interop)

## Architecture Notes

- **Annotation processing:** `RouteProcessor` generates `io.teavmlambda.generated.GeneratedRouter` from `@Path`/`@GET`/`@POST`/`@PUT`/`@DELETE` annotations at compile time
- **JavaScript interop:** Uses TeaVM JSO (`@JSBody`, `@JSFunctor`, `@Async`) for inline JS, callbacks, and async operations
- **Async DB:** `Db` class wraps Node.js pg.Pool with `@Async`/JSPromise for blocking-style Java code over async JS
- **Request/Response:** Immutable POJOs. Response uses fluent builder pattern (`Response.ok()`, `.status()`, `.header()`, `.body()`)
- **Environment variables:** `DATABASE_URL` (PostgreSQL connection string), `PORT` (Cloud Run, default 8080)
- **Static file serving (Cloud Run):** The `CloudRunAdapter` serves static files from a `./public` directory alongside API routes. Files in `src/main/webapp/` are copied to `target/cloudrun/public/` at build time (via `maven-resources-plugin`). Static files are checked first on GET requests; if no file matches, the request falls through to the Java router. Supports `index.html` directory defaults, MIME type detection, and path traversal protection.

## Code Conventions

- Standard Maven layout: `src/main/java/`
- Resource classes use JAX-RS-style annotations (`@Path("/users")`, `@GET`, `@PathParam("id")`, `@Body`)
- No test framework — integration tests only via shell scripts
- No CI/CD pipeline configured
