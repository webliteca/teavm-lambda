# Kotlin DSL Reference

> Kotlin-idiomatic DSL for teavm-lambda. **JVM profiles only** (`jvm-server`, `jvm-war`) — not compatible with TeaVM/JS compilation.
> Module: `teavm-lambda-kotlin` (depends on `teavm-lambda-core` and `teavm-lambda-db-api`).

---

## Entry Points

### `app { }` — Start the platform

```kotlin
fun main() = app {
    cors()
    healthCheck("/health")
    services { /* DI registrations */ }
    routes { /* route definitions */ }
}
```

Builds middleware + routes, calls `Platform.start(router)`.

### `router { }` — Build without starting

```kotlin
val router = router {
    routes { "/test" { get { ok("works") } } }
}
```

Returns a `Router` for testing or embedding in servlet containers.

---

## Routing DSL

Routes are defined inside `routes { }` using nested path strings and HTTP method handlers.

```kotlin
routes {
    "/items" {
        get { ok(service<ItemService>().list()) }
        post { created(service<ItemService>().create(body(CreateItemRequest))) }

        "/{id}" {
            get { ok(service<ItemService>().find(pathInt("id")) ?: throw NotFound()) }
            put {
                val req = body(UpdateItemRequest)
                ok(service<ItemService>().update(pathInt("id"), req))
            }
            delete { service<ItemService>().delete(pathInt("id")); noContent() }
        }
    }
}
```

Available HTTP methods: `get`, `post`, `put`, `patch`, `delete`, `head`, `options`.

Each method also has a sub-path variant: `get("/sub") { ... }`.

Path parameters use `{name}` syntax, extracted via `path("name")` or `pathInt("name")`.

---

## RequestContext (handler receiver)

Every route handler runs with `RequestContext` as the receiver. It provides:

### Path parameters
- `path(name): String` — raw value, throws `BadRequest` if missing
- `pathInt(name): Int` — auto-converted, throws `BadRequest` on bad format
- `pathLong(name): Long`

### Query parameters
- `query(name): String?` — nullable
- `query(name, default): String`
- `queryInt(name): Int?`
- `queryInt(name, default): Int`

### Headers
- `header(name): String?`
- `header(name, default): String`

### Body
- `body: String?` — raw body
- `requireBody(): String` — throws `BadRequest` if absent
- `bodyJson(): JsonReader` — parse as JSON
- `body(deserializer): T` — deserialize via `JsonDeserializer<T>`

### DI Container
- `service<T>(): T` — resolve service (reified)
- `service(type): T` — resolve by class

### Response helpers
- `ok(body: JsonSerializable): Response` — 200 JSON
- `ok(body: String): Response` — 200 JSON string
- `ok(items: List<JsonSerializable>): Response` — 200 JSON array
- `ok { "key" to "value" }` — 200 with inline JSON builder
- `created(body): Response` — 201
- `noContent(): Response` — 204
- `status(code): Response` — custom status
- `respond(code) { header(...); json { ... } }` — custom response builder

---

## Services / DI

```kotlin
services {
    bind(Database::class.java) { DatabaseFactory.create(env("DATABASE_URL")) }
    singleton<ItemService> { ItemService(get()) }
    instance(myConfig)
}
```

- `bind(type, factory)` — register a factory
- `singleton<T> { factory }` — register singleton (reified)
- `instance<T>(value)` — register existing instance (reified)
- `get<T>(): T` — resolve from container (for wiring)
- `env(name)` / `env(name, default)` — environment variable access

---

## Middleware

```kotlin
app {
    cors()                              // permissive defaults
    cors { allowOrigin("https://example.com"); allowCredentials(true) }
    healthCheck("/health")              // 200 OK on GET /health
    healthCheck("/up", """{"ok":true}""")

    middleware { request, next ->       // custom middleware
        val start = System.currentTimeMillis()
        val response = next(request)
        response.header("X-Duration", "${System.currentTimeMillis() - start}ms")
    }

    use(myJavaMiddleware)               // add a Java Middleware instance
}
```

---

## JSON DSL

### Building JSON objects

```kotlin
val s = json {
    "name" to "Alice"
    "age" to 30
    "active" to true
    "address" to json { "city" to "Toronto" }
    "tags" to jsonArray { addString("kotlin"); addString("serverless") }
    "item" to myJsonSerializable        // embeds toJson() output
    raw("prebuilt", someRawJsonString)   // embed pre-serialized JSON
}
```

### Building JSON arrays

```kotlin
val arr = jsonArray {
    addString("a")
    add(myItem)                          // JsonSerializable
    addAll(itemList)                     // Iterable<JsonSerializable>
    addObject { "key" to "value" }       // inline object
}
// arr is JsonArrayValue — use in json { "items" to arr }

val arrString = jsonArrayString { ... }  // returns String directly
```

### JsonSerializable interface

Implement on data classes for automatic serialization in `ok()`, `created()`, list responses:

```kotlin
data class Item(val id: Int, val name: String) : JsonSerializable {
    override fun toJson() = json { "id" to id; "name" to name }
}
```

### JsonDeserializer interface

Implement on companion objects for `body(Item)` deserialization:

```kotlin
data class Item(val id: Int, val name: String) : JsonSerializable {
    companion object : JsonDeserializer<Item> {
        override fun fromJson(json: String): Item {
            val r = JsonReader.parse(json)
            return Item(r.getInt("id", 0), r.getString("name"))
        }
    }
    override fun toJson() = json { "id" to id; "name" to name }
}

// In a handler:
val item = body(Item)
```

---

## Validation DSL

```kotlin
post {
    val req = body(CreateItemRequest)
    validate {
        require(req.name.isNotEmpty()) { "name" to "must not be empty" }
        notEmpty(req.name, "name")
        notBlank(req.description, "description")
        min(req.quantity, 0, "quantity")
        max(req.quantity, 10_000, "quantity")
        range(req.priority, 1..5, "priority")
        matches(req.email, Regex("^.+@.+$"), "email")
        length(req.name, min = 1, max = 100, field = "name")
    }
    created(service<ItemService>().create(req))
}
```

`validate { }` collects all errors and throws `ValidationException` (caught by the router, returns 400 with `{"errors":[{"field":"...","message":"..."}]}`).

Use `validationResult { }` to get a `ValidationResult` without throwing.

---

## HTTP Exceptions

Throw from any handler — caught by the router, converted to RFC 7807 ProblemDetail responses:

| Exception | Status |
|-----------|--------|
| `BadRequest(msg)` | 400 |
| `Unauthorized(msg)` | 401 |
| `Forbidden(msg)` | 403 |
| `NotFound(msg)` | 404 |
| `MethodNotAllowed(msg)` | 405 |
| `Conflict(msg)` | 409 |
| `UnprocessableEntity(msg)` | 422 |
| `TooManyRequests(msg)` | 429 |
| `InternalError(msg)` | 500 |

All extend `HttpException(status, message)`. All have a default message matching the status text.

---

## Database Extensions

### DbRow extensions

```kotlin
row.stringOrNull("column")     // String? (null-safe)
row.int("column", default)     // Int with default
row.double("column", default)  // Double with default
row.bool("column", default)    // Boolean with default
row["column"]                  // operator: same as stringOrNull
```

### DbResult extensions

```kotlin
result.map(Item)               // map all rows via RowMapper
result.map { it.getString("name") }  // map with lambda
result.firstOrNull(Item)       // first row or null
result.first(Item, "Not found") // first row or throw NotFound
result.isEmpty / result.isNotEmpty
```

### Database query extensions

```kotlin
db.queryAll("SELECT * FROM items", Item)
db.queryAll("SELECT * FROM items WHERE active = $1", "true", mapper = Item)
db.queryOne("SELECT * FROM items WHERE id = $1", id.toString(), mapper = Item)
```

### RowMapper interface

Implement on companion objects for `result.map(Item)` pattern:

```kotlin
data class Item(val id: Int, val name: String) : JsonSerializable {
    companion object : RowMapper<Item>, JsonDeserializer<Item> {
        override fun fromRow(row: DbRow) = Item(row.getInt("id"), row.getString("name"))
        override fun fromJson(json: String): Item { /* ... */ }
    }
    override fun toJson() = json { "id" to id; "name" to name }
}
```

---

## Annotation-based Kotlin (without the DSL)

The Kotlin DSL is optional. You can use teavm-lambda annotations directly in Kotlin, exactly as in Java:

```kotlin
@Path("/todos")
@Component
@Singleton
class TodoResource @Inject constructor(private val todoService: TodoService) {

    @GET
    fun list(): Response = Response.ok(todoService.getAll()).header("Content-Type", "application/json")

    @POST
    fun create(@Body body: String): Response { /* ... */ }
}

fun main() {
    val container = GeneratedContainer()
    container.register(Database::class.java, DatabaseFactory.create(Platform.env("DATABASE_URL")))
    Platform.start(GeneratedRouter(container))
}
```

This approach works with all profiles including TeaVM/JS (if you avoid Kotlin-stdlib-only APIs that TeaVM doesn't support). The DSL module (`teavm-lambda-kotlin`) is JVM-only.
