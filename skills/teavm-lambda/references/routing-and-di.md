# Routing and Dependency Injection

> Read this when the user is defining resource classes, wiring dependencies, adding validation, or setting up OpenAPI documentation.

## Routing

Resource classes use JAX-RS-style annotations. The annotation processor generates `GeneratedRouter` at compile time — an if-else chain with zero reflection.

### Path binding

```java
@Path("/users")      // class-level base path
@Component
@Singleton
public class UsersResource {

    @GET                          // GET /users
    public Response list() { ... }

    @GET
    @Path("/{id}")                // GET /users/{id}
    public Response getById(@PathParam("id") String id) { ... }

    @POST                         // POST /users
    public Response create(@Body String body) { ... }

    @DELETE
    @Path("/{id}")                // DELETE /users/{id}
    public Response delete(@PathParam("id") String id) { ... }
}
```

### Parameter binding

| Annotation | Source | Type | Example |
|-----------|--------|------|---------|
| `@PathParam("id")` | URL path segment `{id}` | String | `/users/{id}` |
| `@QueryParam("q")` | Query string `?q=value` | String | `/search?q=java` |
| `@HeaderParam("X-Request-Id")` | HTTP header (case-insensitive) | String | Custom headers |
| `@Body` | Entire request body | String | JSON body |
| `SecurityContext` | JWT claims (no annotation) | SecurityContext | Injected when auth is configured |

All parameters are strings. Type conversion is your responsibility. For numeric path params, parse with `Integer.parseInt()` etc.

`@Body String body` is the **only** body binding. There is no automatic JSON-to-POJO deserialization. Parse with `JsonReader.parse(body)`.

### HTTP methods

`@GET`, `@POST`, `@PUT`, `@DELETE`, `@PATCH`, `@HEAD`, `@OPTIONS` — one per method. Combine with `@Path` for sub-paths.

### Return type

All resource methods must return `Response`:

```java
@GET
public Response list() {
    return Response.ok(jsonArray)
            .header("Content-Type", "application/json");
}

@POST
public Response create(@Body String body) {
    // ... create entity ...
    return Response.status(201)
            .header("Content-Type", "application/json")
            .body(entity.toJson());
}

@DELETE
@Path("/{id}")
public Response delete(@PathParam("id") String id) {
    return Response.status(204);
}
```

---

## Dependency Injection

teavm-lambda uses compile-time DI. The annotation processor generates `GeneratedContainer` which wires all components based on `@Inject` constructors. Zero reflection at runtime.

### Stereotype annotations

| Annotation | Meaning |
|-----------|---------|
| `@Component` | General DI-managed component |
| `@Service` | Business logic (semantic alias for @Component) |
| `@Repository` | Data access (semantic alias for @Component) |
| `@Singleton` | Single shared instance per container |
| `@Inject` | Marks constructor for DI wiring |

### Constructor injection

```java
@Service
@Singleton
public class UserService {
    private final UserRepository repo;

    @Inject
    public UserService(UserRepository repo) {
        this.repo = repo;
    }
}
```

### External dependency registration

External objects (Database, JwtValidator, etc.) are not annotation-scanned. Register them on the Container **before** constructing GeneratedRouter:

```java
Container container = new GeneratedContainer();
container.register(Database.class, DatabaseFactory.create(dbUrl));
container.register(JwtValidator.class, JwtValidatorFactory.create());

Router router = new GeneratedRouter(container);
Platform.start(router);
```

### Resource classes are components

Resource classes need `@Component` (or `@Service`/`@Repository`) to be discovered by the annotation processor. Add `@Singleton` if they should be shared instances (usual case):

```java
@Path("/users")
@Component
@Singleton
public class UsersResource {
    private final UserService service;

    @Inject
    public UsersResource(UserService service) {
        this.service = service;
    }
    // ...
}
```

### Circular dependency detection

The annotation processor detects circular dependencies at compile time and fails with a clear error message. Break cycles by restructuring your components.

---

## Validation

Validation annotations on resource method parameters generate checks in `GeneratedRouter`. If validation fails, a 400 Bad Request response is returned automatically with error details.

### Annotations

```java
@POST
public Response create(
        @NotNull @Body String body,                    // must not be null
        @NotEmpty @QueryParam("name") String name,     // must not be null or empty
        @Min(1) @QueryParam("page") String page,       // numeric minimum
        @Max(100) @QueryParam("limit") String limit,   // numeric maximum
        @Pattern("[a-z]+") @QueryParam("slug") String slug  // regex match
) { ... }
```

All validation annotations accept an optional `message` parameter for custom error messages.

### Manual validation

For complex validation beyond annotations, use `ValidationResult`:

```java
@POST
public Response create(@Body String body) {
    JsonReader json = JsonReader.parse(body);
    ValidationResult v = new ValidationResult();
    if (!json.has("name")) v.addError("name", "is required");
    if (!json.has("email")) v.addError("email", "is required");
    if (!v.isValid()) return v.toResponse();  // 400 with errors JSON
    // ...
}
```

---

## OpenAPI and Swagger UI

Annotate resource classes to auto-generate OpenAPI 3.0.3 specs:

```java
@Path("/items")
@Component @Singleton
@ApiTag(value = "Items", description = "Item management")
@ApiInfo(title = "My API", version = "1.0.0", description = "My REST API")
public class ItemsResource {

    @GET
    @ApiOperation(summary = "List all items")
    @ApiResponse(code = 200, description = "Item list", mediaType = "application/json")
    public Response list() { ... }

    @POST
    @ApiOperation(summary = "Create an item")
    @ApiResponse(code = 201, description = "Created", mediaType = "application/json")
    @ApiResponse(code = 400, description = "Validation error")
    public Response create(@NotNull @Body String body) { ... }
}
```

Auto-generated endpoints (no configuration needed):
- `GET /openapi.json` — OpenAPI 3.0.3 specification
- `GET /swagger-ui` — embedded Swagger UI HTML page
