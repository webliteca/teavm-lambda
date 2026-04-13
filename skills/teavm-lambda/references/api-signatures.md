# teavm-lambda API Signatures

> Read this file when you need exact method signatures for any teavm-lambda public API class.
> This is the authoritative reference — do not guess signatures.

## ca.weblite.teavmlambda.api

### Response (immutable)
- `static Response ok(String body)` — 200 with body
- `static Response status(int statusCode)` — custom status, no body
- `Response header(String name, String value)` — returns new Response with added header
- `Response body(String body)` — returns new Response with string body
- `Response bodyBytes(byte[] bytes)` — returns new Response with binary body
- `int getStatusCode()`
- `Map<String,String> getHeaders()`
- `String getBody()`
- `byte[] getBodyBytes()`

### Request (immutable)
- `Request(String method, String path, Map<String,String> headers, Map<String,String> queryParams, String body)`
- `Request(String method, String path, Map<String,String> headers, Map<String,String> queryParams, Map<String,String> pathParams, String body)`
- `String getMethod()`
- `String getPath()`
- `Map<String,String> getHeaders()`
- `Map<String,String> getQueryParams()`
- `Map<String,String> getPathParams()`
- `String getBody()`
- `Request withPathParams(Map<String,String> pathParams)` — returns new Request

### Router (interface)
- `Response route(Request request)`

### Container
- `<T> void register(Class<T> type, Factory<T> factory)`
- `<T> void register(Class<T> type, T instance)` — register existing instance
- `<T> void registerSingleton(Class<T> type, Factory<T> factory)` — caches after first call
- `<T> T get(Class<T> type)` — throws IllegalStateException if unregistered
- `boolean has(Class<?> type)`
- **Inner interface** `Factory<T>` — `@FunctionalInterface`, method: `T create()`

### Middleware (functional interface)
- `Response handle(Request request, MiddlewareChain chain)`

### MiddlewareChain
- `Response next(Request request)` — advances to next middleware or terminal router

### MiddlewareRouter (implements Router)
- `MiddlewareRouter(Router delegate)` — wraps inner router
- `MiddlewareRouter use(Middleware middleware)` — appends middleware, returns this
- `Response route(Request request)`

### Platform
- `static String env(String name)` — returns empty string if unset
- `static String env(String name, String defaultValue)` — returns default if unset/empty
- `static void start(Router router)` — discovers adapter via ServiceLoader, starts server
- `static void setAdapter(PlatformAdapter adapter)`
- `static boolean isAvailable()`

### PlatformAdapter (interface, SPI)
- `String env(String name)`
- `void start(Router router)`

### ProblemDetail (immutable, RFC 7807)
- `static ProblemDetail of(int status, String detail)`
- `static ProblemDetail badRequest(String detail)` — 400
- `static ProblemDetail notFound(String detail)` — 404
- `static ProblemDetail conflict(String detail)` — 409
- `static ProblemDetail internalError(String detail)` — 500
- `ProblemDetail type(String type)` — returns new with custom type URI
- `ProblemDetail instance(String instance)` — returns new with instance URI
- `String toJson()`
- `Response toResponse()` — Content-Type: application/problem+json
- `String getType()`
- `String getTitle()`
- `int getStatus()`
- `String getDetail()`
- `String getInstance()`

### Resources
- `static void setLoader(ResourceLoader loader)`
- `static boolean isAvailable()`
- `static String loadText(String name)` — load classpath resource

### ResourceLoader (interface, SPI)
- `String loadText(String name)`

---

## ca.weblite.teavmlambda.api.annotation

### Routing
- `@Path(String value)` — TARGET: TYPE, METHOD. URL path segment.
- `@GET` — TARGET: METHOD
- `@POST` — TARGET: METHOD
- `@PUT` — TARGET: METHOD
- `@DELETE` — TARGET: METHOD
- `@PATCH` — TARGET: METHOD
- `@HEAD` — TARGET: METHOD
- `@OPTIONS` — TARGET: METHOD

### Parameter Binding
- `@PathParam(String value)` — TARGET: PARAMETER. Binds to `{name}` in @Path.
- `@QueryParam(String value)` — TARGET: PARAMETER. Binds to query string parameter.
- `@HeaderParam(String value)` — TARGET: PARAMETER. Binds to HTTP header (case-insensitive).
- `@Body` — TARGET: PARAMETER. Entire request body as String.

### Dependency Injection
- `@Component` — TARGET: TYPE. Marks class as DI-managed component.
- `@Service` — TARGET: TYPE. Semantic alias for @Component (business logic).
- `@Repository` — TARGET: TYPE. Semantic alias for @Component (data access).
- `@Singleton` — TARGET: TYPE. Single shared instance per container.
- `@Inject` — TARGET: CONSTRUCTOR. Marks constructor for compile-time DI wiring.

### Validation
- `@NotNull(String message)` — TARGET: PARAMETER. Validates parameter is not null.
- `@NotEmpty(String message)` — TARGET: PARAMETER. Validates string is not null/empty.
- `@Min(long value, String message)` — TARGET: PARAMETER. Numeric minimum.
- `@Max(long value, String message)` — TARGET: PARAMETER. Numeric maximum.
- `@Pattern(String value, String message)` — TARGET: PARAMETER. Regex match.

### Security
- `@RolesAllowed(String[] value)` — TARGET: TYPE, METHOD. Requires one of the listed roles from JWT "groups" claim.
- `@PermitAll` — TARGET: TYPE, METHOD. Allows all (overrides class-level restrictions).
- `@DenyAll` — TARGET: TYPE, METHOD. Denies all access (403).

### OpenAPI
- `@ApiInfo(String title, String version, String description)` — TARGET: TYPE. API metadata.
- `@ApiTag(String value, String description)` — TARGET: TYPE. Tag for grouping endpoints.
- `@ApiOperation(String summary, String description)` — TARGET: METHOD. Operation docs.
- `@ApiResponse(int code, String description, String mediaType)` — TARGET: METHOD. Repeatable.
- `@ApiResponses(ApiResponse[] value)` — TARGET: METHOD. Container for multiple @ApiResponse.

---

## ca.weblite.teavmlambda.api.json

### JsonBuilder (fluent JSON construction)
- `static JsonBuilder object()` — start JSON object
- `static JsonBuilder array()` — start JSON array
- `JsonBuilder put(String key, String value)` — string field (null-safe)
- `JsonBuilder put(String key, int value)`
- `JsonBuilder put(String key, long value)`
- `JsonBuilder put(String key, double value)`
- `JsonBuilder put(String key, boolean value)`
- `JsonBuilder putRaw(String key, String rawJson)` — embed raw JSON value
- `JsonBuilder add(String rawJson)` — add raw JSON element to array
- `JsonBuilder addString(String value)` — add string element to array
- `String build()` — produces JSON string

### JsonReader (lightweight JSON parsing)
- `static JsonReader parse(String json)` — parse JSON object string
- `String getString(String key)` — null if missing
- `String getString(String key, String defaultValue)`
- `int getInt(String key, int defaultValue)`
- `double getDouble(String key, double defaultValue)`
- `boolean getBoolean(String key, boolean defaultValue)`
- `boolean has(String key)` — true if key exists and is non-null

---

## ca.weblite.teavmlambda.api.auth

### JwtValidator (interface)
- `JwtClaims validate(String token) throws JwtValidationException`

### JwtValidatorFactory
- `static JwtValidator create()` — reads JWT_SECRET, JWT_PUBLIC_KEY, JWT_ISSUER, JWT_ALGORITHM, FIREBASE_PROJECT_ID from environment
- `static JwtValidator create(String secret, String issuer, String algorithm)` — explicit config
- `static boolean isAvailable()`
- `static void setProvider(JwtValidatorProvider provider)`

### JwtClaims
- `String getSubject()`
- `String getIssuer()`
- `long getExpirationTime()` — seconds since epoch
- `long getIssuedAt()` — seconds since epoch
- `Set<String> getGroups()` — from JWT "groups" claim
- `String getClaim(String name)`
- `Map<String,String> getAllClaims()`
- `SecurityContext toSecurityContext()`

### SecurityContext
- `SecurityContext(String subject, Set<String> roles, Map<String,String> claims)`
- `String getSubject()` — JWT "sub" claim
- `String getName()` — prefers "upn", then "preferred_username", then "sub"
- `Set<String> getRoles()` — from JWT "groups" claim
- `boolean isUserInRole(String role)`
- `String getClaim(String name)`
- `Map<String,String> getClaims()`

### JwtValidationException
- Extends `Exception`

### JwtValidatorProvider (SPI interface)
- `JwtValidator create(String secret, String issuer, String algorithm)`

---

## ca.weblite.teavmlambda.api.validation

### ValidationResult
- `ValidationResult()` — empty, valid result
- `ValidationResult addError(String field, String message)` — returns this
- `boolean isValid()` — true if no errors
- `List<ValidationError> getErrors()`
- `String toJson()` — `{"errors":[{"field":"...","message":"..."}]}`
- `Response toResponse()` — 400 with JSON body

### ValidationError
- `ValidationError(String field, String message)`
- `String getField()`
- `String getMessage()`
- `String toJson()`

---

## ca.weblite.teavmlambda.api.middleware

### CorsMiddleware (implements Middleware)
- `static Builder builder()` — creates builder with permissive defaults
- **Builder**:
  - `Builder allowOrigin(String origin)` — default: `"*"`
  - `Builder allowMethods(String... methods)` — default: `"GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD"`
  - `Builder allowHeaders(String... headers)` — default: `"Content-Type, Authorization, X-Request-Id"`
  - `Builder allowCredentials(boolean allow)` — default: false
  - `Builder maxAge(int seconds)` — default: 86400
  - `CorsMiddleware build()`

### CompressionMiddleware (implements Middleware)
- `CompressionMiddleware()` — discovers Compressor via SPI
- `CompressionMiddleware(Compressor compressor)` — explicit compressor
- Compresses responses >256 bytes with text-based Content-Type when Accept-Encoding includes gzip/deflate

---

## ca.weblite.teavmlambda.api.health

### HealthEndpoint (implements Middleware)
- `HealthEndpoint()` — no checks initially
- `HealthEndpoint add(String name, HealthCheck check)` — returns this
- Intercepts `GET /health`, `GET /health/ready`, `GET /health/live`
- Returns 200 if all checks pass, 503 if any fail

### HealthCheck (functional interface)
- `HealthResult check()`

### HealthResult
- `static HealthResult up()`
- `static HealthResult up(Map<String,String> details)`
- `static HealthResult down(String error)`
- `static HealthResult down(Throwable t)`
- `boolean isUp()`
- `Status getStatus()` — enum: UP, DOWN
- `Map<String,String> getDetails()`
- `String toJson()`

---

## ca.weblite.teavmlambda.api.compression

### Compressor (interface, SPI)
- `String gzip(String data)`
- `String deflate(String data)`

### CompressorFactory
- `static Compressor create()` — discovers via ServiceLoader
- `static void setProvider(CompressorProvider provider)`

### CompressorProvider (SPI interface)
- `Compressor create()`

---

## ca.weblite.teavmlambda.api.db

### Database (interface)
- `DbResult query(String sql)`
- `DbResult query(String sql, String... params)` — params bind to $1, $2, etc.
- `void close()`

### DatabaseFactory
- `static Database create(String connectionUrl)` — e.g. `"postgresql://user:pass@host:5432/db"`
- `static boolean isAvailable()`
- `static void setProvider(DatabaseProvider provider)`

### DatabaseProvider (SPI interface)
- `Database create(String connectionUrl)`

### DbResult (interface)
- `List<DbRow> getRows()`
- `int getRowCount()`
- `default String toJsonArray()` — serializes all rows as JSON array

### DbRow (interface)
- `String getString(String column)`
- `int getInt(String column)`
- `double getDouble(String column)`
- `boolean getBoolean(String column)`
- `boolean has(String column)`
- `boolean isNull(String column)`
- `String toJson()` — serializes row as JSON object

### JsonProvider (SPI interface)
- JSON parsing support for database layer

---

## ca.weblite.teavmlambda.api.nosqldb

### NoSqlClient (interface)
- `String get(String collection, String id)` — returns JSON string or null
- `void put(String collection, String id, String json)`
- `void delete(String collection, String id)`
- `List<String> query(String collection, String field, String operator, String value)` — operators: `=`, `<`, `>`, `<=`, `>=`, `begins_with`
- `List<String> list(String collection)` — all documents as JSON strings

### NoSqlClientFactory
- `static NoSqlClient create(String uri)` — `"dynamodb://us-east-1"`, `"dynamodb://localhost:8000"`, `"firestore://project-id"`

### NoSqlProvider (SPI interface)
- `NoSqlClient create(String uri)`

---

## ca.weblite.teavmlambda.api.objectstore

### ObjectStoreClient (interface)
- `void putObject(String bucket, String key, String data, String contentType)`
- `String getObject(String bucket, String key)` — returns UTF-8 string or null
- `void deleteObject(String bucket, String key)`
- `List<String> listObjects(String bucket, String prefix)`
- `boolean objectExists(String bucket, String key)`
- `void putObjectBytes(String bucket, String key, byte[] data, String contentType)`
- `byte[] getObjectBytes(String bucket, String key)` — returns bytes or null

### ObjectStoreClientFactory
- `static ObjectStoreClient create(String uri)` — `"s3://us-east-1"`, `"s3://localhost:9000"`, `"gcs://project-id"`

### ObjectStoreProvider (SPI interface)
- `ObjectStoreClient create(String uri)`

---

## ca.weblite.teavmlambda.api.messagequeue

### MessageQueueClient (interface)
- `String sendMessage(String queueUrl, String messageBody)` — returns message ID
- `List<Message> receiveMessages(String queueUrl, int maxMessages)` — maxMessages 1-10
- `void deleteMessage(String queueUrl, String receiptHandle)`
- `int getMessageCount(String queueUrl)` — approximate count, -1 if unsupported

### Message
- `String getMessageId()`
- `String getBody()`
- `String getReceiptHandle()`

### MessageQueueClientFactory
- `static MessageQueueClient create(String uri)` — `"sqs://us-east-1"`, `"sqs://localhost:9324"`, `"pubsub://project-id"`

### MessageQueueProvider (SPI interface)
- `MessageQueueClient create(String uri)`

---

## ca.weblite.teavmlambda.impl.jvm.war

### WarServlet (abstract, extends HttpServlet)
- `protected abstract Router createRouter()` — called once during init()
- Annotate subclass with `@WebServlet(urlPatterns = "/*")`
- Do NOT call `Platform.start()` — the servlet container manages the lifecycle

---

## ca.weblite.teavmlambda.dsl (Kotlin DSL — JVM only)

### Top-level functions
- `fun app(block: AppScope.() -> Unit)` — configure and start the platform
- `fun router(block: AppScope.() -> Unit): Router` — build router without starting
- `fun json(block: JsonObjectScope.() -> Unit): String` — build JSON object string
- `fun jsonArray(block: JsonArrayScope.() -> Unit): JsonArrayValue` — build JSON array (for embedding)
- `fun jsonArrayString(block: JsonArrayScope.() -> Unit): String` — build JSON array as raw string
- `fun validate(block: ValidationScope.() -> Unit)` — validate and throw on errors
- `fun validationResult(block: ValidationScope.() -> Unit): ValidationResult` — validate without throwing
- `fun Iterable<JsonSerializable>.toJsonArray(): String` — serialize list to JSON array

### AppScope
- `fun env(name: String): String`
- `fun env(name: String, default: String): String`
- `fun cors(block: CorsMiddleware.Builder.() -> Unit = {})` — add CORS middleware
- `fun healthCheck(path: String = "/health", body: String = ...)` — add health endpoint
- `fun middleware(handler: (Request, (Request) -> Response) -> Response)` — custom middleware
- `fun use(middleware: Middleware)` — add Java Middleware
- `fun services(block: ServiceScope.() -> Unit)` — configure DI
- `fun routes(block: RoutingScope.() -> Unit)` — define routes

### ServiceScope
- `fun <T> bind(type: Class<T>, factory: () -> T)`
- `fun <T> singleton(type: Class<T>, factory: () -> T)`
- `inline fun <reified T> singleton(noinline factory: () -> T)`
- `fun <T> instance(type: Class<T>, instance: T)`
- `inline fun <reified T> instance(instance: T)`
- `fun <T> get(type: Class<T>): T`
- `inline fun <reified T> get(): T`
- `fun env(name: String): String`

### RoutingScope
- `operator fun String.invoke(block: RoutingScope.() -> Unit)` — nested path group
- `fun get(handler: RouteHandler)` / `fun get(path: String, handler: RouteHandler)`
- `fun post(handler)` / `fun post(path, handler)`
- `fun put(handler)` / `fun put(path, handler)`
- `fun patch(handler)` / `fun patch(path, handler)`
- `fun delete(handler)` / `fun delete(path, handler)`
- `fun head(handler)` / `fun options(handler)`

### RequestContext (handler receiver)
- `val request: Request` / `val container: Container`
- `fun path(name: String): String` — path param, throws BadRequest if missing
- `fun pathInt(name: String): Int` — path param as Int
- `fun pathLong(name: String): Long`
- `fun query(name: String): String?` / `fun query(name: String, default: String): String`
- `fun queryInt(name: String): Int?` / `fun queryInt(name: String, default: Int): Int`
- `fun header(name: String): String?` / `fun header(name: String, default: String): String`
- `val body: String?` / `fun requireBody(): String` / `fun bodyJson(): JsonReader`
- `fun <T> body(deserializer: JsonDeserializer<T>): T`
- `inline fun <reified T> service(): T` / `fun <T> service(type: Class<T>): T`
- `fun ok(body: JsonSerializable): Response` / `fun ok(body: String): Response`
- `fun ok(items: List<JsonSerializable>): Response` / `fun ok(block: JsonObjectScope.() -> Unit): Response`
- `fun created(body: JsonSerializable): Response` / `fun created(body: String): Response`
- `fun noContent(): Response` / `fun status(code: Int): Response`
- `fun respond(statusCode: Int, block: ResponseScope.() -> Unit): Response`

### JsonObjectScope
- `infix fun String.to(value: String?)` / `infix fun String.to(value: Int)` / `infix fun String.to(value: Long)`
- `infix fun String.to(value: Double)` / `infix fun String.to(value: Boolean)`
- `infix fun String.to(block: JsonObjectScope)` — nested object
- `infix fun String.to(array: JsonArrayValue)` — nested array
- `infix fun String.to(value: JsonSerializable?)` — embed serializable
- `fun raw(key: String, rawJson: String?)` — embed pre-serialized JSON

### JsonArrayScope
- `fun add(rawJson: String)` / `fun addString(value: String?)` / `fun add(value: JsonSerializable)`
- `fun addAll(values: Iterable<JsonSerializable>)` / `fun addObject(block: JsonObjectScope.() -> Unit)`

### JsonSerializable (interface)
- `fun toJson(): String`

### JsonDeserializer\<T\> (interface)
- `fun fromJson(json: String): T`

### RowMapper\<T\> (interface)
- `fun fromRow(row: DbRow): T`

### HttpException (sealed class, extends RuntimeException)
- `val status: Int` / `override val message: String`
- `fun toProblemDetail(): ProblemDetail` / `fun toResponse(): Response`

### Concrete exceptions (all extend HttpException)
- `BadRequest(message = "Bad Request")` — 400
- `Unauthorized(message = "Unauthorized")` — 401
- `Forbidden(message = "Forbidden")` — 403
- `NotFound(message = "Not Found")` — 404
- `MethodNotAllowed(message = "Method Not Allowed")` — 405
- `Conflict(message = "Conflict")` — 409
- `UnprocessableEntity(message = "Unprocessable Entity")` — 422
- `TooManyRequests(message = "Too Many Requests")` — 429
- `InternalError(message = "Internal Server Error")` — 500

### ValidationScope
- `inline fun require(condition: Boolean, error: () -> Pair<String, String>)`
- `fun notEmpty(value: String?, field: String, message: String = ...)`
- `fun notBlank(value: String?, field: String, message: String = ...)`
- `fun min(value: Int, min: Int, field: String, message: String = ...)`
- `fun max(value: Int, max: Int, field: String, message: String = ...)`
- `fun min(value: Long, min: Long, field: String, message: String = ...)`
- `fun max(value: Long, max: Long, field: String, message: String = ...)`
- `fun range(value: Int, range: IntRange, field: String, message: String = ...)`
- `fun matches(value: String?, pattern: Regex, field: String, message: String = ...)`
- `fun length(value: String?, min: Int = 0, max: Int = MAX_VALUE, field: String, message: String = ...)`

### ValidationResult
- `val errors: List<FieldError>` / `val isValid: Boolean`
- `fun throwIfInvalid()` / `fun toJson(): String`

### DbRow extensions
- `fun DbRow.stringOrNull(column: String): String?`
- `fun DbRow.int(column: String, default: Int = 0): Int`
- `fun DbRow.double(column: String, default: Double = 0.0): Double`
- `fun DbRow.bool(column: String, default: Boolean = false): Boolean`
- `operator fun DbRow.get(column: String): String?`

### DbResult extensions
- `fun <T> DbResult.map(mapper: RowMapper<T>): List<T>`
- `fun <T> DbResult.map(transform: (DbRow) -> T): List<T>`
- `fun <T> DbResult.firstOrNull(mapper: RowMapper<T>): T?`
- `fun <T> DbResult.firstOrNull(transform: (DbRow) -> T): T?`
- `fun <T> DbResult.first(mapper: RowMapper<T>, message: String = "Not found"): T`
- `val DbResult.isEmpty: Boolean` / `val DbResult.isNotEmpty: Boolean`

### Database extensions
- `fun <T> Database.queryAll(sql: String, mapper: RowMapper<T>): List<T>`
- `fun <T> Database.queryAll(sql: String, vararg params: String, mapper: RowMapper<T>): List<T>`
- `fun <T> Database.queryOne(sql: String, vararg params: String, mapper: RowMapper<T>): T?`
