# Gotchas (Full List)

> Read this when debugging unexpected behavior or before starting a new teavm-lambda project.

## 1. @Body String is the only body binding

There is no automatic JSON-to-POJO deserialization. The `@Body` annotation binds the raw request body as a `String`.

**Wrong:**
```java
@POST
public Response create(@Body User user) {  // WRONG — will not work
```

**Correct:**
```java
@POST
public Response create(@Body String body) {
    JsonReader json = JsonReader.parse(body);
    String name = json.getString("name");
    String email = json.getString("email");
    // ...
}
```

## 2. No reflection — ever

teavm-lambda uses compile-time annotation processing. TeaVM does not support Java reflection. Never suggest or use:
- Jackson (`ObjectMapper`)
- Gson
- Any library that uses `Class.forName()`, `Method.invoke()`, or field reflection

Use `JsonBuilder` for serialization and `JsonReader` for deserialization.

**Wrong:**
```java
ObjectMapper mapper = new ObjectMapper();  // WRONG — reflection-based
User user = mapper.readValue(body, User.class);
```

**Correct:**
```java
JsonReader r = JsonReader.parse(body);
User user = new User(r.getString("name"), r.getInt("age", 0));
```

## 3. Response is immutable

`Response.header()` and `Response.body()` return **new** Response instances. The original is unchanged.

**Wrong:**
```java
Response response = Response.ok(body);
response.header("Content-Type", "application/json");  // WRONG — return value discarded
return response;  // missing Content-Type header
```

**Correct:**
```java
return Response.ok(body)
        .header("Content-Type", "application/json");
// or:
Response response = Response.ok(body);
response = response.header("Content-Type", "application/json");
return response;
```

## 4. Package locations for middleware classes

- `MiddlewareRouter` is in `ca.weblite.teavmlambda.api`
- `CorsMiddleware`, `CompressionMiddleware` are in `ca.weblite.teavmlambda.api.middleware`
- `HealthEndpoint` is in `ca.weblite.teavmlambda.api.health`

```java
import ca.weblite.teavmlambda.api.MiddlewareRouter;
import ca.weblite.teavmlambda.api.middleware.CorsMiddleware;
import ca.weblite.teavmlambda.api.middleware.CompressionMiddleware;
import ca.weblite.teavmlambda.api.health.HealthEndpoint;
```

## 5. PostgreSQL parameterized queries use $1, $2

The Database interface uses PostgreSQL-style positional parameters, not JDBC-style `?`.

**Wrong:**
```java
db.query("SELECT * FROM users WHERE id = ?", id);  // WRONG
```

**Correct:**
```java
db.query("SELECT * FROM users WHERE id = $1", id);
db.query("INSERT INTO users (name, email) VALUES ($1, $2)", name, email);
```

## 6. @Singleton is teavm-lambda's annotation

Import from `ca.weblite.teavmlambda.api.annotation.Singleton`, not from javax or jakarta.

**Wrong:**
```java
import javax.inject.Singleton;      // WRONG
import jakarta.inject.Singleton;    // WRONG
```

**Correct:**
```java
import ca.weblite.teavmlambda.api.annotation.Singleton;
```

The same applies to all DI annotations — they are teavm-lambda's own, not javax/jakarta.

## 7. Register external dependencies before constructing GeneratedRouter

`GeneratedRouter` resolves dependencies from the Container during construction. If an external dependency (Database, JwtValidator, etc.) is not yet registered, it will throw `IllegalStateException`.

**Wrong:**
```java
Container container = new GeneratedContainer();
Router router = new GeneratedRouter(container);  // WRONG — Database not registered yet
container.register(Database.class, db);           // too late
```

**Correct:**
```java
Container container = new GeneratedContainer();
container.register(Database.class, db);           // register first
Router router = new GeneratedRouter(container);   // then construct router
```

## 8. WAR deployments: don't call Platform.start()

In WAR deployments, the servlet container manages the HTTP lifecycle. Subclass `WarServlet` and override `createRouter()` instead.

**Wrong:**
```java
// In a WAR deployment:
Platform.start(router);  // WRONG — throws UnsupportedOperationException or conflicts with servlet container
```

**Correct:**
```java
@WebServlet(urlPatterns = "/*")
public class MyServlet extends WarServlet {
    @Override
    protected Router createRouter() {
        Container container = new GeneratedContainer();
        // ... register dependencies ...
        return new GeneratedRouter(container);
    }
}
```

## 9. All parameters are strings

`@PathParam`, `@QueryParam`, `@HeaderParam`, and `@Body` all bind as `String`. Parse to other types yourself:

```java
@GET
@Path("/{id}")
public Response getById(@PathParam("id") String id) {
    int numericId = Integer.parseInt(id);  // manual conversion
    // ...
}
```

## 10. Content-Type header is not set automatically

You must set the Content-Type header explicitly on every JSON response.

**Wrong:**
```java
return Response.ok(jsonString);  // Content-Type defaults to nothing
```

**Correct:**
```java
return Response.ok(jsonString)
        .header("Content-Type", "application/json");
```

## 11. Static files only on Cloud Run and JVM HttpServer

The Lambda adapters do not serve static files. Static file serving is only available with:
- `teavm-lambda-adapter-cloudrun`
- `teavm-lambda-adapter-httpserver`

For Lambda, serve static files via S3 + CloudFront or API Gateway binary support.

## 12. Compression requires a provider module

`CompressionMiddleware` discovers a `Compressor` via ServiceLoader. Add the compression provider to your dependencies:
- Node.js: `teavm-lambda-compression`
- JVM: `teavm-lambda-compression-jvm`

Without the provider, `CompressionMiddleware` will throw at startup.
