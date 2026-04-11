# CLAUDE.md

## Project Overview

teavm-lambda is a Java framework for building serverless HTTP APIs that deploy to AWS Lambda, Google Cloud Run, standalone JVM servers, and Servlet containers (Tomcat, TomEE, Jetty). It follows a Write Once Run Anywhere (WORA) architecture — the same application code compiles to JavaScript via TeaVM (for Node.js) or native JVM bytecode without modification. Maven profiles control the compilation target.

**Version:** 0.1.0-SNAPSHOT  
**Package:** `ca.weblite.teavmlambda`

## Build & Run

**Prerequisites:** JDK 21, Maven 3.9+, Docker, Docker Compose

```bash
# Build everything (default TeaVM/Node.js profile)
mvn clean package

# Build a specific module and its dependencies
mvn clean package -pl teavm-lambda-demo-features -am

# Build with JVM standalone server profile
mvn clean package -pl teavm-lambda-demo-features -am -P jvm

# Build as WAR for Servlet container deployment
mvn clean package -pl teavm-lambda-demo-features -am -P jvm-war

# Local dev (Lambda demo): start PostgreSQL, then SAM
docker compose up -d
sam local start-api --template template.yaml
```

### Cloud Run Demo (with static files)

```bash
cd teavm-lambda-demo-cloudrun
docker compose up
# Open http://localhost:8081
```

## Test

Testing is done via integration test scripts that use Docker Compose + Docker containers.

```bash
# Features demo integration tests (Cloud Run / TeaVM)
./teavm-lambda-demo-features/run-tests.sh

# Features demo WAR integration tests (Tomcat)
./teavm-lambda-demo-features/run-war-tests.sh

# Lambda demo integration tests
mvn clean package -pl teavm-lambda-demo -am
./teavm-lambda-demo/run-tests.sh

# Cloud Run demo tests
./teavm-lambda-demo-cloudrun/run-tests.sh

# Manual testing against running server
./teavm-lambda-demo/test.sh http://localhost:3001
```

Test database: PostgreSQL 16, credentials `demo/demo`, schema in init.sql files under `docker/` directories.

## Project Structure

Multi-module Maven project:

### Core Framework
| Module | Purpose | Platform |
|--------|---------|----------|
| `teavm-lambda-core` | Core annotations, Request/Response, Router, Container, Middleware, Platform, JSON utilities, ProblemDetail, validation | All |
| `teavm-lambda-core-js` | Node.js SPI implementations (ResourceLoader, etc.) | Node.js |
| `teavm-lambda-core-jvm` | JVM SPI implementations | JVM |
| `teavm-lambda-processor` | JSR 269 annotation processor — generates `GeneratedRouter` and `GeneratedContainer` at compile time | Build-time |

### Platform Adapters
| Module | Purpose | Platform |
|--------|---------|----------|
| `teavm-lambda-adapter-lambda` | AWS Lambda / API Gateway adapter (TeaVM/JS) | Node.js |
| `teavm-lambda-adapter-lambda-jvm` | AWS Lambda / API Gateway adapter (JVM) | JVM |
| `teavm-lambda-adapter-cloudrun` | Google Cloud Run HTTP server adapter (TeaVM/JS) | Node.js |
| `teavm-lambda-adapter-httpserver` | JDK built-in HttpServer adapter (zero external dependencies) | JVM |
| `teavm-lambda-adapter-war` | Servlet adapter for WAR deployment (Tomcat, TomEE, Jetty) | JVM |

### Database & Cloud Services
| Module | Purpose | Platform |
|--------|---------|----------|
| `teavm-lambda-db-api` | Platform-neutral Database, DbResult, DbRow interfaces | All |
| `teavm-lambda-db` | Node.js pg driver implementation via JSO | Node.js |
| `teavm-lambda-db-jvm` | JDBC database implementation | JVM |
| `teavm-lambda-nosqldb` | NoSQL database API (DynamoDB, Firestore) | All |
| `teavm-lambda-dynamodb` | AWS DynamoDB implementation | Node.js |
| `teavm-lambda-firestore` | Google Firestore implementation | Node.js |
| `teavm-lambda-objectstore` | Object storage API (S3, GCS) | All |
| `teavm-lambda-s3` / `teavm-lambda-s3-jvm` | AWS S3 implementations | Node.js / JVM |
| `teavm-lambda-gcs` / `teavm-lambda-gcs-jvm` | Google Cloud Storage implementations | Node.js / JVM |
| `teavm-lambda-messagequeue` | Message queue API (SQS, Pub/Sub) | All |
| `teavm-lambda-sqs` / `teavm-lambda-sqs-jvm` | AWS SQS implementations | Node.js / JVM |
| `teavm-lambda-pubsub` / `teavm-lambda-pubsub-jvm` | Google Pub/Sub implementations | Node.js / JVM |

### Cross-Cutting
| Module | Purpose |
|--------|---------|
| `teavm-lambda-auth` / `teavm-lambda-auth-jvm` | JWT authentication (@RolesAllowed, @PermitAll, @DenyAll) |
| `teavm-lambda-compression` / `teavm-lambda-compression-jvm` | Response compression middleware |
| `teavm-lambda-image-api` / `teavm-lambda-image` / `teavm-lambda-image-jvm` | Image processing API |
| `teavm-lambda-logging` | Logging abstraction |
| `teavm-lambda-sentry` | Sentry error tracking integration |

### Demos
| Module | Purpose |
|--------|---------|
| `teavm-lambda-demo` | Demo REST API for AWS Lambda |
| `teavm-lambda-demo-cloudrun` | Demo REST API + static web UI for Cloud Run |
| `teavm-lambda-demo-features` | Full-featured demo (middleware, CORS, health, validation, compression, OpenAPI) |
| `teavm-lambda-demo-features-lambda` | Lambda variant of the features demo |
| `teavm-lambda-demo-auth` | Authentication demo |
| `teavm-lambda-demo-nosqldb` | NoSQL database demo |
| `teavm-lambda-demo-objectstore` | Object storage demo |
| `teavm-lambda-demo-messagequeue` | Message queue demo |

## Key Dependencies

- **TeaVM 0.10.2** — Java-to-JavaScript compiler (classlib, jso, jso-apis, maven-plugin)
- **Node.js pg ^8.13.0** — PostgreSQL client (runtime, accessed via JSO interop)
- **PostgreSQL JDBC 42.7.3** — JDBC driver for JVM profiles
- **Jakarta Servlet API 6.0.0** — Servlet API for WAR deployment (provided scope)

## Architecture Notes

- **WORA:** Application code depends only on platform-neutral API modules. Implementations (Node.js or JVM) are discovered at runtime via `ServiceLoader`
- **Annotation processing:** `RouteProcessor` generates `ca.weblite.teavmlambda.generated.GeneratedRouter` and `GeneratedContainer` from `@Path`/`@GET`/`@POST`/`@PUT`/`@DELETE`/`@PATCH`/`@Component`/`@Service`/`@Repository`/`@Inject` annotations at compile time
- **Platform abstraction:** `Platform.start(router)` discovers the correct adapter via ServiceLoader. `Platform.env()` reads environment variables portably. For WAR deployment, `Platform.start()` is not used — the servlet container manages the lifecycle
- **JavaScript interop:** Uses TeaVM JSO (`@JSBody`, `@JSFunctor`, `@Async`) for inline JS, callbacks, and async operations
- **Request/Response:** Immutable POJOs. Response uses fluent builder pattern (`Response.ok()`, `.status()`, `.header()`, `.body()`)
- **Middleware:** Composable pipeline via `MiddlewareRouter` — CORS, compression, health checks, custom middleware
- **Dependency injection:** `@Component`, `@Service`, `@Repository`, `@Singleton`, `@Inject` with compile-time wiring (zero reflection)
- **Validation:** `@NotNull`, `@NotEmpty`, `@Min`, `@Max`, `@Pattern` parameter validation
- **Security:** JWT validation with `@RolesAllowed`, `@PermitAll`, `@DenyAll`
- **OpenAPI:** Auto-generated OpenAPI 3.0 spec and Swagger UI from annotations
- **Environment variables:** `DATABASE_URL` (PostgreSQL connection string), `PORT` (Cloud Run/standalone, default 8080)
- **Static file serving:** Cloud Run and HttpServer adapters serve static files from `./public` directory alongside API routes

## Maven Profiles

| Profile | Compilation | Output | Adapter |
|---------|------------|--------|---------|
| `teavm` (default) | TeaVM → JavaScript | `target/lambda/` or `target/cloudrun/` | `teavm-lambda-adapter-lambda` or `teavm-lambda-adapter-cloudrun` |
| `jvm` | javac → bytecode, maven-shade uber JAR | Single JAR in `target/` | `teavm-lambda-adapter-lambda-jvm` |
| `jvm-server` | javac → bytecode, maven-shade uber JAR | Single JAR in `target/` | `teavm-lambda-adapter-httpserver` |
| `jvm-war` | javac → bytecode, maven-war WAR | WAR file in `target/` | `teavm-lambda-adapter-war` |

## Claude Code Skill

A Claude Code skill is packaged from `skills/teavm-lambda/` via the `skills-jar-plugin` (published on Maven Central). The skill JAR is built automatically during `mvn package` and deployed alongside the library artifacts.

### Building the skill JAR

```bash
mvn clean package
# Produces target/teavm-lambda-parent-0.1.0-SNAPSHOT-skills.jar
```

### Installing the skill

```bash
# From source (into another project)
cp -r skills/teavm-lambda /path/to/project/.claude/skills/teavm-lambda

# From source (user-level, all projects)
cp -r skills/teavm-lambda ~/.claude/skills/teavm-lambda

# From GitHub Packages JAR
mvn dependency:copy -Dartifact=ca.weblite:teavm-lambda-parent:0.1.0-SNAPSHOT:jar:skills \
  -DoutputDirectory=/tmp/teavm-lambda-skill -Dmdep.stripVersion=true
mkdir -p ~/.claude/skills/teavm-lambda
unzip -o /tmp/teavm-lambda-skill/teavm-lambda-parent-skills.jar -d ~/.claude/skills/teavm-lambda
```

### Skill contents

- `SKILL.md` — main skill definition (quickstart, annotations, API reference, middleware, deployment)
- `references/` — 12 deep-dive docs (API signatures, pom.xml templates, routing, security, database, deployments, gotchas)
- `assets/examples/` — 4 starter projects (minimal-hello, crud-postgres, jwt-protected, cloudrun-deploy)
- `scripts/` — `generate-api-signatures.sh` regenerates `references/api-signatures.md` from source

## Code Conventions

- Standard Maven layout: `src/main/java/`
- Package: `ca.weblite.teavmlambda` (API in `api`, implementations in `impl.js` or `impl.jvm`)
- Resource classes use JAX-RS-style annotations (`@Path("/users")`, `@GET`, `@PathParam("id")`, `@Body`)
- SPI symmetry pattern: each API module has JS and JVM implementation modules
- No test framework — integration tests only via shell scripts and Docker
- No CI/CD pipeline configured
