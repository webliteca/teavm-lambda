---
name: teavm-lambda
description: "Use this skill whenever the user is building, deploying, or debugging a teavm-lambda application — even if they only mention building a serverless Java app for Lambda or Cloud Run. Triggers on: teavm-lambda, TeaVM Lambda, Java serverless, compile Java to JavaScript for Lambda, JAX-RS Cloud Run, Java REST API serverless, zero-reflection Java framework, Java to Node.js compiler, WORA Java framework, Java compile-time DI."
globs:
  - "**/*.java"
  - "**/*.kt"
  - "**/pom.xml"
  - "**/template.yaml"
  - "**/Dockerfile"
---

# teavm-lambda

Java framework for building serverless REST APIs. Compiles Java 21 to JavaScript via TeaVM and runs on Node.js 22 — or compiles to JVM bytecode. Same code deploys to AWS Lambda, Google Cloud Run, standalone JVM servers, and Servlet containers (Tomcat, TomEE, Jetty). Write Once Run Anywhere (WORA): Maven profiles control the compilation target.

**Version**: 0.1.0-SNAPSHOT
**Zero reflection**: all routing, DI, and validation are generated at compile time by an annotation processor.

## Mental Model

The annotation processor scans `@Path`/`@GET`/`@Component`/`@Inject` etc. at compile time and generates two classes:
- `GeneratedRouter` — an if-else chain matching HTTP method + path, extracting parameters, validating, enforcing security, invoking resource methods. No reflection.
- `GeneratedContainer` — wires all `@Component`/`@Service`/`@Repository` classes with their `@Inject` constructor dependencies. Detects circular dependencies at compile time.

Application code depends only on platform-neutral API modules. JS and JVM implementations are discovered at runtime via `ServiceLoader`.

## Quickstart

```java
// HelloResource.java
package com.example.myapp;

import ca.weblite.teavmlambda.api.Response;
import ca.weblite.teavmlambda.api.annotation.*;
import ca.weblite.teavmlambda.api.json.JsonBuilder;

@Path("/hello")
@Component
@Singleton
public class HelloResource {

    @GET
    public Response hello() {
        return Response.ok(JsonBuilder.object().put("message", "Hello!").build())
                .header("Content-Type", "application/json");
    }

    @GET
    @Path("/{name}")
    public Response helloName(@PathParam("name") String name) {
        return Response.ok(JsonBuilder.object().put("message", "Hello, " + name + "!").build())
                .header("Content-Type", "application/json");
    }
}
```

```java
// Main.java
package com.example.myapp;

import ca.weblite.teavmlambda.api.*;
import ca.weblite.teavmlambda.generated.*;

public class Main {
    public static void main(String[] args) {
        Container container = new GeneratedContainer();
        Router router = new GeneratedRouter(container);
        Platform.start(router);
    }
}
```

Build and run (JVM standalone):
```bash
mvn clean package -P jvm-server
java -jar target/my-app-1.0-SNAPSHOT.jar
# Listening on http://localhost:8080
```

## Core API Quick Reference

### Response (immutable — chain or reassign)
```
Response.ok(body)                   → 200 with body
Response.status(code)               → custom status
.header(name, value)                → new Response with header
.body(body)                         → new Response with body
.bodyBytes(bytes)                   → new Response with binary body
.getStatusCode() / .getHeaders() / .getBody() / .getBodyBytes()
```

### Request (immutable)
```
.getMethod() / .getPath() / .getHeaders() / .getQueryParams() / .getPathParams() / .getBody()
.withPathParams(map)                → new Request with different path params
```

### Container
```
container.register(Class, instance) → register external dependency
container.register(Class, factory)  → register factory
container.registerSingleton(Class, factory) → caches after first call
container.get(Class)                → retrieve instance
container.has(Class)                → check if registered
```

### Platform
```
Platform.env("VAR")                 → environment variable (empty string if unset)
Platform.env("VAR", "default")      → with fallback
Platform.start(router)              → discover adapter via ServiceLoader, start server
```

### JsonBuilder
```
JsonBuilder.object().put("k", "v").put("n", 42).putRaw("nested", json).build()
JsonBuilder.array().add(rawJson).addString("s").build()
```

### JsonReader
```
JsonReader r = JsonReader.parse(body);
r.getString("key")                  → String or null
r.getString("key", "default")
r.getInt("key", 0) / r.getDouble("key", 0.0) / r.getBoolean("key", false)
r.has("key")                        → true if present and non-null
```

### ProblemDetail (RFC 7807)
```
ProblemDetail.badRequest("msg").toResponse()    → 400
ProblemDetail.notFound("msg").toResponse()      → 404
ProblemDetail.conflict("msg").toResponse()      → 409
ProblemDetail.internalError("msg").toResponse() → 500
ProblemDetail.of(code, "detail").type("uri").instance("uri").toResponse()
```

### ValidationResult
```
ValidationResult v = new ValidationResult();
v.addError("field", "message");
if (!v.isValid()) return v.toResponse();  → 400 with {"errors":[...]}
```

## Annotations

| Annotation | Target | Purpose |
|-----------|--------|---------|
| `@Path("/x")` | TYPE, METHOD | URL path segment |
| `@GET` `@POST` `@PUT` `@DELETE` `@PATCH` `@HEAD` `@OPTIONS` | METHOD | HTTP method |
| `@PathParam("id")` | PARAMETER | Bind path variable |
| `@QueryParam("q")` | PARAMETER | Bind query string param |
| `@HeaderParam("X-Id")` | PARAMETER | Bind HTTP header |
| `@Body` | PARAMETER | Bind entire request body (String) |
| `@Component` | TYPE | DI-managed component |
| `@Service` | TYPE | Alias for @Component (business logic) |
| `@Repository` | TYPE | Alias for @Component (data access) |
| `@Singleton` | TYPE | Shared instance per container |
| `@Inject` | CONSTRUCTOR | Constructor injection |
| `@NotNull` `@NotEmpty` | PARAMETER | Null/empty validation |
| `@Min(n)` `@Max(n)` | PARAMETER | Numeric range validation |
| `@Pattern("regex")` | PARAMETER | Regex validation |
| `@RolesAllowed({"role"})` | TYPE, METHOD | Require JWT role |
| `@PermitAll` | TYPE, METHOD | Allow unauthenticated |
| `@DenyAll` | TYPE, METHOD | Block all (403) |
| `@ApiInfo` `@ApiTag` `@ApiOperation` `@ApiResponse` | TYPE/METHOD | OpenAPI docs |

All annotations are in `ca.weblite.teavmlambda.api.annotation`.

## Middleware

```java
Router router = new MiddlewareRouter(new GeneratedRouter(container))
        .use(new HealthEndpoint().add("db", () -> { db.query("SELECT 1"); return HealthResult.up(); }))
        .use(CorsMiddleware.builder().allowCredentials(true).build())
        .use(new CompressionMiddleware());
Platform.start(router);
```

- `MiddlewareRouter` is in `ca.weblite.teavmlambda.api`
- `CorsMiddleware`, `CompressionMiddleware` are in `ca.weblite.teavmlambda.api.middleware`
- `HealthEndpoint` is in `ca.weblite.teavmlambda.api.health`

## Database (PostgreSQL)

```java
Database db = DatabaseFactory.create(Platform.env("DATABASE_URL", "postgresql://demo:demo@localhost:5432/demo"));
container.register(Database.class, db);
```

```java
DbResult result = db.query("SELECT * FROM users WHERE id = $1", userId);
DbRow row = result.getRows().get(0);
String name = row.getString("name");
String allJson = result.toJsonArray();
```

Modules: `teavm-lambda-db-api` + `teavm-lambda-db` (JS) or `teavm-lambda-db-jvm` + `postgresql:42.7.3` (JVM).

## Security

```java
container.register(JwtValidator.class, JwtValidatorFactory.create());
```

Env vars: `JWT_SECRET` (HMAC) or `JWT_ALGORITHM=firebase` + `FIREBASE_PROJECT_ID`.
Modules: `teavm-lambda-auth` (JS) or `teavm-lambda-auth-jvm` (JVM).

```java
@RolesAllowed({"user"})
public Response me(SecurityContext ctx) {
    String userId = ctx.getSubject();
    String name = ctx.getName();
    boolean admin = ctx.isUserInRole("admin");
}
```

## Cloud Services

| Service | Factory | URI Examples |
|---------|---------|-------------|
| NoSQL | `NoSqlClientFactory.create(uri)` | `dynamodb://us-east-1`, `firestore://project-id` |
| Object Storage | `ObjectStoreClientFactory.create(uri)` | `s3://us-east-1`, `gcs://project-id` |
| Message Queue | `MessageQueueClientFactory.create(uri)` | `sqs://us-east-1`, `pubsub://project-id` |

Register each on the Container. See `references/nosql-objectstore-mq.md` for full API.

## Deployment Targets

| Profile | Compilation | Adapter Module | Output | Run |
|---------|------------|----------------|--------|-----|
| `teavm` (default) | Java→JS via TeaVM | `adapter-lambda` | `target/lambda/` | `sam local start-api` |
| `teavm` + cloudrun | Java→JS via TeaVM | `adapter-cloudrun` | `target/cloudrun/` | `node server.js` |
| `jvm` | javac→shade JAR | `adapter-lambda-jvm` | `target/*.jar` | Lambda Java 21 |
| `jvm-server` | javac→shade JAR | `adapter-httpserver` | `target/*.jar` | `java -jar` |
| `jvm-war` | javac→WAR | `adapter-war` | `target/*.war` | Tomcat 10.1+ |

## Kotlin DSL (JVM only)

The `teavm-lambda-kotlin` module provides a Kotlin-idiomatic DSL for routing, middleware, DI, JSON, validation, and database access. It targets JVM profiles only (`jvm-server`, `jvm-war`) — not TeaVM/JS.

```kotlin
fun main() = app {
    cors()
    healthCheck("/health")

    services {
        bind(Database::class.java) { DatabaseFactory.create(env("DATABASE_URL")) }
        singleton<ItemService> { ItemService(get()) }
    }

    routes {
        "/items" {
            get { ok(service<ItemService>().list()) }
            post {
                val req = body(CreateItemRequest)
                validate {
                    notEmpty(req.name, "name")
                    min(req.quantity, 0, "quantity")
                }
                created(service<ItemService>().create(req))
            }
            "/{id}" {
                get { ok(service<ItemService>().find(pathInt("id")) ?: throw NotFound()) }
                delete { service<ItemService>().delete(pathInt("id")); noContent() }
            }
        }
    }
}
```

Key features: `app { }` / `router { }` entry points, nested path routing, `RequestContext` with `path()`, `pathInt()`, `query()`, `body()`, `service<T>()`, `ok()`, `created()`, `noContent()` helpers, `json { }` / `jsonArray { }` DSL, `validate { }` DSL, typed HTTP exceptions (`NotFound()`, `BadRequest()`, etc.), and `DbRow`/`DbResult` extensions.

Dependency: `teavm-lambda-kotlin` (brings in `teavm-lambda-core` and `teavm-lambda-db-api`).

See `references/kotlin-dsl.md` for the full API reference.

## Top Gotchas

1. **`@Body String` is the only body binding.** No automatic JSON-to-POJO deserialization. Parse with `JsonReader.parse(body)`.

2. **No reflection, ever.** Do not use Jackson, Gson, ObjectMapper, or any reflective library. Use `JsonBuilder` for serialization and `JsonReader` for deserialization.

3. **Response is immutable.** `.header()` and `.body()` return new instances. Chain them: `Response.ok(body).header("Content-Type", "application/json")` — or reassign: `response = response.header(...)`.

4. **Package locations.** `MiddlewareRouter` is in `ca.weblite.teavmlambda.api`. `CorsMiddleware`/`CompressionMiddleware` are in `ca.weblite.teavmlambda.api.middleware`. `HealthEndpoint` is in `ca.weblite.teavmlambda.api.health`.

5. **PostgreSQL params use `$1`, `$2`** — not `?`. `db.query("SELECT * FROM users WHERE id = $1", id)`.

6. **`@Singleton` is teavm-lambda's annotation** — `ca.weblite.teavmlambda.api.annotation.Singleton`, not javax or jakarta.

7. **Register external deps before constructing GeneratedRouter.** `container.register(Database.class, db)` must come before `new GeneratedRouter(container)`.

8. **WAR: don't call `Platform.start()`.** Subclass `WarServlet`, override `createRouter()`, annotate with `@WebServlet(urlPatterns = "/*")`.

## Environment Variables

| Variable | Purpose | Default |
|----------|---------|---------|
| `DATABASE_URL` | PostgreSQL connection string | — |
| `PORT` | HTTP listen port (Cloud Run / JVM server) | `8080` |
| `JWT_SECRET` | HMAC secret for JWT validation | — |
| `JWT_PUBLIC_KEY` | PEM public key for RS256 | — |
| `JWT_ALGORITHM` | `HS256`, `RS256`, or `firebase` | `HS256` |
| `JWT_ISSUER` | Expected JWT issuer | — |
| `FIREBASE_PROJECT_ID` | Firebase project ID | — |
| `SENTRY_DSN` | Sentry error tracking DSN | — |

## Reference Files

Read these for in-depth coverage:

- **Kotlin DSL** → `references/kotlin-dsl.md` — app{}, routing, JSON, validation, DB extensions
- **API signatures** → `references/api-signatures.md` — every public class and method
- **Routing & DI** → `references/routing-and-di.md` — @Path, parameters, DI wiring, validation, OpenAPI
- **Middleware** → `references/middleware.md` — CORS, compression, health checks, custom middleware
- **Database** → `references/database.md` — PostgreSQL queries, repository pattern, entity pattern
- **Cloud services** → `references/nosql-objectstore-mq.md` — DynamoDB, Firestore, S3, GCS, SQS, Pub/Sub
- **Security** → `references/security-jwt.md` — JWT, @RolesAllowed, SecurityContext, Firebase Auth
- **Deploy to Lambda** → `references/deployment-lambda.md` — SAM template, TeaVM + JVM profiles
- **Deploy to Cloud Run** → `references/deployment-cloudrun.md` — Dockerfile, static files
- **Deploy JVM standalone** → `references/deployment-jvm.md` — jvm-server profile, HttpServer
- **Deploy as WAR** → `references/deployment-war.md` — WarServlet, Tomcat/Jetty/TomEE
- **POM templates** → `references/pom-templates.md` — copy-pasteable pom.xml for each target
- **All gotchas** → `references/gotchas.md` — full list with wrong/correct code examples

## Example Projects

Copy and adapt these complete, runnable projects:

- `assets/examples/minimal-hello/` — smallest working app (Main + HelloResource + pom.xml)
- `assets/examples/crud-postgres/` — CRUD with PostgreSQL repository pattern
- `assets/examples/jwt-protected/` — JWT auth with @RolesAllowed and SecurityContext
- `assets/examples/cloudrun-deploy/` — Cloud Run deployment with Dockerfile
