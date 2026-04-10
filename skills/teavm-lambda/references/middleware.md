# Middleware

> Read this when the user needs to add CORS, compression, health checks, or custom request/response interceptors.

## MiddlewareRouter

Wraps a router with a chain of middleware. Each middleware can inspect/modify the request, delegate to the next middleware via `chain.next(request)`, and inspect/modify the response.

```java
Router router = new MiddlewareRouter(new GeneratedRouter(container))
        .use(healthEndpoint)
        .use(CorsMiddleware.builder().allowCredentials(true).build())
        .use(new CompressionMiddleware());
Platform.start(router);
```

Execution order: middleware added first runs first for requests, last for responses (onion model).

## Middleware interface

```java
@FunctionalInterface
public interface Middleware {
    Response handle(Request request, MiddlewareChain chain);
}
```

Call `chain.next(request)` to continue the chain. Return a `Response` directly to short-circuit.

## CorsMiddleware

Handles preflight OPTIONS requests and adds CORS headers to all responses.

```java
CorsMiddleware cors = CorsMiddleware.builder()
        .allowOrigin("https://example.com")       // default: "*"
        .allowMethods("GET", "POST", "PUT", "DELETE")  // default: all common methods
        .allowHeaders("Content-Type", "Authorization") // default: Content-Type, Authorization, X-Request-Id
        .allowCredentials(true)                    // default: false
        .maxAge(3600)                              // default: 86400
        .build();
```

Package: `ca.weblite.teavmlambda.api.middleware.CorsMiddleware`

## CompressionMiddleware

Compresses responses based on `Accept-Encoding` header. Supports gzip and deflate.

```java
.use(new CompressionMiddleware())
```

Only compresses when:
- Response body > 256 bytes
- Content-Type is text-based (text/*, json, xml, javascript)
- Client sends Accept-Encoding header

**Required dependency**: `teavm-lambda-compression` (Node.js) or `teavm-lambda-compression-jvm` (JVM).

Package: `ca.weblite.teavmlambda.api.middleware.CompressionMiddleware`

## HealthEndpoint

Middleware that intercepts `GET /health`, `GET /health/ready`, and `GET /health/live`. Returns 200 if all checks pass, 503 if any fail.

```java
HealthEndpoint health = new HealthEndpoint()
        .add("database", () -> {
            try {
                db.query("SELECT 1");
                return HealthResult.up();
            } catch (Exception e) {
                return HealthResult.down(e);
            }
        })
        .add("cache", () -> HealthResult.up());

Router router = new MiddlewareRouter(new GeneratedRouter(container))
        .use(health);
```

Response format:
```json
{
  "status": "UP",
  "checks": {
    "database": {"status": "UP"},
    "cache": {"status": "UP"}
  }
}
```

Package: `ca.weblite.teavmlambda.api.health.HealthEndpoint`

## Custom middleware example

```java
public class LoggingMiddleware implements Middleware {
    public Response handle(Request request, MiddlewareChain chain) {
        long start = System.currentTimeMillis();
        Response response = chain.next(request);
        long ms = System.currentTimeMillis() - start;
        System.out.println(request.getMethod() + " " + request.getPath()
                + " -> " + response.getStatusCode() + " (" + ms + "ms)");
        return response;
    }
}
```

## Middleware as lambda

Since `Middleware` is a `@FunctionalInterface`, you can use lambdas:

```java
.use((request, chain) -> {
    Response response = chain.next(request);
    return response.header("X-Powered-By", "teavm-lambda");
})
```
