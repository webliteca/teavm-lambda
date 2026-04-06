package ca.weblite.teavmlambda.dsl

import ca.weblite.teavmlambda.api.Container
import ca.weblite.teavmlambda.api.Request
import ca.weblite.teavmlambda.api.Response
import ca.weblite.teavmlambda.api.json.JsonReader

/**
 * Receiver scope for route handlers. Provides ergonomic access to
 * request parameters, body parsing, DI container, and response helpers.
 *
 * ```kotlin
 * get("/{id}") {
 *     val id: Int = path("id")     // auto-converted to Int
 *     val item = service<ItemService>().find(id) ?: throw NotFound("Item $id not found")
 *     ok(item)
 * }
 * ```
 */
class RequestContext(
    /** The underlying Java Request. */
    val request: Request,
    /** The DI container for service resolution. */
    val container: Container
) {

    // ── Path parameters ──────────────────────────────────────────────

    /** Get a path parameter as a String. Throws [BadRequest] if missing. */
    fun path(name: String): String =
        request.pathParams[name] ?: throw BadRequest("Missing path parameter: $name")

    /** Get a path parameter converted to Int. Throws [BadRequest] on missing or bad format. */
    fun pathInt(name: String): Int =
        path(name).toIntOrNull() ?: throw BadRequest("Path parameter '$name' must be an integer")

    /** Get a path parameter converted to Long. */
    fun pathLong(name: String): Long =
        path(name).toLongOrNull() ?: throw BadRequest("Path parameter '$name' must be a long")

    // ── Query parameters ─────────────────────────────────────────────

    /** Get an optional query parameter. */
    fun query(name: String): String? = request.queryParams[name]

    /** Get a query parameter with a default. */
    fun query(name: String, default: String): String = request.queryParams[name] ?: default

    /** Get a query parameter as Int, or null. */
    fun queryInt(name: String): Int? = request.queryParams[name]?.toIntOrNull()

    /** Get a query parameter as Int with default. */
    fun queryInt(name: String, default: Int): Int = request.queryParams[name]?.toIntOrNull() ?: default

    // ── Headers ──────────────────────────────────────────────────────

    /** Get a request header (case-sensitive lookup). */
    fun header(name: String): String? = request.headers[name]

    /** Get a request header with a default value. */
    fun header(name: String, default: String): String = request.headers[name] ?: default

    // ── Body ─────────────────────────────────────────────────────────

    /** The raw request body string, or null. */
    val body: String?
        get() = request.body

    /** The raw request body, throwing [BadRequest] if absent. */
    fun requireBody(): String =
        request.body ?: throw BadRequest("Request body is required")

    /** Parse the request body as a JSON object via [JsonReader]. */
    fun bodyJson(): JsonReader = JsonReader.parse(requireBody())

    /**
     * Deserialize the request body using a [JsonDeserializer].
     *
     * ```kotlin
     * val item = body(Item)  // Item companion object implements JsonDeserializer<Item>
     * ```
     */
    fun <T> body(deserializer: JsonDeserializer<T>): T = deserializer.fromJson(requireBody())

    // ── DI container ─────────────────────────────────────────────────

    /** Resolve a service from the DI container by class. */
    fun <T> service(type: Class<T>): T = container.get(type)

    /** Resolve a service from the DI container (reified). */
    inline fun <reified T> service(): T = service(T::class.java)

    // ── Response helpers ─────────────────────────────────────────────

    /** 200 OK with a [JsonSerializable] body. */
    fun ok(body: JsonSerializable): Response =
        Response.ok(body.toJson()).header("Content-Type", "application/json")

    /** 200 OK with a raw string body (application/json). */
    fun ok(body: String): Response =
        Response.ok(body).header("Content-Type", "application/json")

    /** 200 OK with a list of [JsonSerializable]. */
    fun ok(items: List<JsonSerializable>): Response =
        Response.ok(items.toJsonArray()).header("Content-Type", "application/json")

    /** 200 OK with an inline JSON builder. */
    fun ok(block: JsonObjectScope.() -> Unit): Response =
        ok(json(block))

    /** 201 Created with a [JsonSerializable] body. */
    fun created(body: JsonSerializable): Response =
        Response.status(201).header("Content-Type", "application/json").body(body.toJson())

    /** 201 Created with a raw JSON string body. */
    fun created(body: String): Response =
        Response.status(201).header("Content-Type", "application/json").body(body)

    /** 204 No Content. */
    fun noContent(): Response = Response.status(204)

    /** Custom status code response. */
    fun status(code: Int): Response = Response.status(code)

    /** Build a response with a DSL. */
    fun respond(statusCode: Int = 200, block: ResponseScope.() -> Unit): Response {
        val scope = ResponseScope(statusCode)
        scope.block()
        return scope.build()
    }
}

/**
 * DSL scope for building custom responses.
 *
 * ```kotlin
 * respond(201) {
 *     header("Location", "/items/42")
 *     json {
 *         "id" to 42
 *         "name" to "New Item"
 *     }
 * }
 * ```
 */
class ResponseScope(private val statusCode: Int) {
    private val headers = mutableMapOf<String, String>()
    private var body: String? = null

    fun header(name: String, value: String) {
        headers[name] = value
    }

    fun body(text: String) {
        this.body = text
    }

    fun json(block: JsonObjectScope.() -> Unit) {
        headers.putIfAbsent("Content-Type", "application/json")
        val scope = JsonObjectScope()
        scope.block()
        this.body = scope.build()
    }

    internal fun build(): Response {
        var response = Response.status(statusCode)
        for ((k, v) in headers) {
            response = response.header(k, v)
        }
        if (body != null) {
            response = response.body(body)
        }
        return response
    }
}

/**
 * Interface for companion objects that can deserialize from JSON.
 *
 * ```kotlin
 * data class Item(val id: Int, val name: String) : JsonSerializable {
 *     companion object : JsonDeserializer<Item> {
 *         override fun fromJson(json: String): Item {
 *             val r = JsonReader.parse(json)
 *             return Item(r.getInt("id", 0), r.getString("name"))
 *         }
 *     }
 *     override fun toJson() = json { "id" to id; "name" to name }
 * }
 *
 * // In a handler:
 * val item = body(Item)
 * ```
 */
interface JsonDeserializer<T> {
    fun fromJson(json: String): T
}
