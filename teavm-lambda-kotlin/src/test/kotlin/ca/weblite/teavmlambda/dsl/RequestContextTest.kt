package ca.weblite.teavmlambda.dsl

import ca.weblite.teavmlambda.api.Container
import ca.weblite.teavmlambda.api.Request
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class RequestContextTest {

    private fun request(
        method: String = "GET",
        path: String = "/test",
        headers: Map<String, String> = emptyMap(),
        queryParams: Map<String, String> = emptyMap(),
        pathParams: Map<String, String> = emptyMap(),
        body: String? = null
    ) = Request(method, path, headers, queryParams, pathParams, body)

    private fun ctx(
        method: String = "GET",
        path: String = "/test",
        headers: Map<String, String> = emptyMap(),
        queryParams: Map<String, String> = emptyMap(),
        pathParams: Map<String, String> = emptyMap(),
        body: String? = null,
        container: Container = Container()
    ) = RequestContext(request(method, path, headers, queryParams, pathParams, body), container)

    // ── Path parameters ──────────────────────────────────────────────

    @Test
    fun `path returns path parameter`() {
        val ctx = ctx(pathParams = mapOf("id" to "42"))
        assertEquals("42", ctx.path("id"))
    }

    @Test
    fun `path throws BadRequest for missing parameter`() {
        val ctx = ctx()
        val ex = assertFailsWith<BadRequest> { ctx.path("id") }
        assertEquals(400, ex.status)
    }

    @Test
    fun `pathInt converts to Int`() {
        val ctx = ctx(pathParams = mapOf("id" to "42"))
        assertEquals(42, ctx.pathInt("id"))
    }

    @Test
    fun `pathInt throws BadRequest for non-numeric value`() {
        val ctx = ctx(pathParams = mapOf("id" to "abc"))
        assertFailsWith<BadRequest> { ctx.pathInt("id") }
    }

    @Test
    fun `pathLong converts to Long`() {
        val ctx = ctx(pathParams = mapOf("id" to "9999999999"))
        assertEquals(9999999999L, ctx.pathLong("id"))
    }

    @Test
    fun `pathLong throws BadRequest for non-numeric value`() {
        val ctx = ctx(pathParams = mapOf("id" to "not-a-long"))
        assertFailsWith<BadRequest> { ctx.pathLong("id") }
    }

    // ── Query parameters ─────────────────────────────────────────────

    @Test
    fun `query returns parameter value`() {
        val ctx = ctx(queryParams = mapOf("page" to "2"))
        assertEquals("2", ctx.query("page"))
    }

    @Test
    fun `query returns null for missing parameter`() {
        val ctx = ctx()
        assertNull(ctx.query("missing"))
    }

    @Test
    fun `query with default returns default when missing`() {
        val ctx = ctx()
        assertEquals("1", ctx.query("page", "1"))
    }

    @Test
    fun `query with default returns actual value when present`() {
        val ctx = ctx(queryParams = mapOf("page" to "5"))
        assertEquals("5", ctx.query("page", "1"))
    }

    @Test
    fun `queryInt returns parsed int`() {
        val ctx = ctx(queryParams = mapOf("limit" to "10"))
        assertEquals(10, ctx.queryInt("limit"))
    }

    @Test
    fun `queryInt returns null for missing param`() {
        val ctx = ctx()
        assertNull(ctx.queryInt("limit"))
    }

    @Test
    fun `queryInt returns null for non-numeric value`() {
        val ctx = ctx(queryParams = mapOf("limit" to "abc"))
        assertNull(ctx.queryInt("limit"))
    }

    @Test
    fun `queryInt with default returns default when missing`() {
        val ctx = ctx()
        assertEquals(25, ctx.queryInt("limit", 25))
    }

    // ── Headers ──────────────────────────────────────────────────────

    @Test
    fun `header returns header value`() {
        val ctx = ctx(headers = mapOf("X-Request-Id" to "abc123"))
        assertEquals("abc123", ctx.header("X-Request-Id"))
    }

    @Test
    fun `header returns null for missing header`() {
        val ctx = ctx()
        assertNull(ctx.header("X-Missing"))
    }

    @Test
    fun `header with default returns default when missing`() {
        val ctx = ctx()
        assertEquals("none", ctx.header("X-Missing", "none"))
    }

    // ── Body ─────────────────────────────────────────────────────────

    @Test
    fun `body property returns raw body`() {
        val ctx = ctx(body = """{"name":"test"}""")
        assertEquals("""{"name":"test"}""", ctx.body)
    }

    @Test
    fun `body property returns null when no body`() {
        val ctx = ctx()
        assertNull(ctx.body)
    }

    @Test
    fun `requireBody returns body when present`() {
        val ctx = ctx(body = "content")
        assertEquals("content", ctx.requireBody())
    }

    @Test
    fun `requireBody throws BadRequest when body is null`() {
        val ctx = ctx()
        assertFailsWith<BadRequest> { ctx.requireBody() }
    }

    @Test
    fun `bodyJson parses JSON body`() {
        val ctx = ctx(body = """{"name":"Alice","age":30}""")
        val reader = ctx.bodyJson()
        assertEquals("Alice", reader.getString("name"))
        assertEquals(30, reader.getInt("age", 0))
    }

    @Test
    fun `body with JsonDeserializer deserializes body`() {
        data class Item(val name: String)
        val deserializer = object : JsonDeserializer<Item> {
            override fun fromJson(json: String): Item {
                val r = ca.weblite.teavmlambda.api.json.JsonReader.parse(json)
                return Item(r.getString("name"))
            }
        }
        val ctx = ctx(body = """{"name":"Widget"}""")
        val item = ctx.body(deserializer)
        assertEquals("Widget", item.name)
    }

    // ── DI container ─────────────────────────────────────────────────

    @Test
    fun `service resolves from container`() {
        val container = Container()
        container.register(String::class.java, "hello-service")
        val ctx = RequestContext(request(), container)
        assertEquals("hello-service", ctx.service(String::class.java))
    }

    // ── Response helpers ─────────────────────────────────────────────

    @Test
    fun `ok with string returns 200 with json content type`() {
        val ctx = ctx()
        val response = ctx.ok("""{"status":"ok"}""")
        assertEquals(200, response.statusCode)
        assertEquals("application/json", response.headers["Content-Type"])
        assertEquals("""{"status":"ok"}""", response.body)
    }

    @Test
    fun `ok with JsonSerializable returns serialized body`() {
        val item = object : JsonSerializable {
            override fun toJson() = """{"id":1}"""
        }
        val ctx = ctx()
        val response = ctx.ok(item)
        assertEquals(200, response.statusCode)
        assertEquals("""{"id":1}""", response.body)
    }

    @Test
    fun `ok with list of JsonSerializable returns array`() {
        val items = listOf(
            object : JsonSerializable { override fun toJson() = """{"id":1}""" },
            object : JsonSerializable { override fun toJson() = """{"id":2}""" }
        )
        val ctx = ctx()
        val response = ctx.ok(items)
        assertEquals("""[{"id":1},{"id":2}]""", response.body)
    }

    @Test
    fun `ok with block builds JSON inline`() {
        val ctx = ctx()
        val response = ctx.ok { "status" to "healthy" }
        assertEquals("""{"status":"healthy"}""", response.body)
    }

    @Test
    fun `created returns 201 with body`() {
        val item = object : JsonSerializable {
            override fun toJson() = """{"id":42}"""
        }
        val ctx = ctx()
        val response = ctx.created(item)
        assertEquals(201, response.statusCode)
        assertEquals("""{"id":42}""", response.body)
    }

    @Test
    fun `created with string returns 201`() {
        val ctx = ctx()
        val response = ctx.created("""{"id":1}""")
        assertEquals(201, response.statusCode)
        assertEquals("application/json", response.headers["Content-Type"])
    }

    @Test
    fun `noContent returns 204 with no body`() {
        val ctx = ctx()
        val response = ctx.noContent()
        assertEquals(204, response.statusCode)
        assertNull(response.body)
    }

    @Test
    fun `status returns custom status code`() {
        val ctx = ctx()
        val response = ctx.status(202)
        assertEquals(202, response.statusCode)
    }

    @Test
    fun `respond builds custom response with DSL`() {
        val ctx = ctx()
        val response = ctx.respond(201) {
            header("Location", "/items/42")
            json {
                "id" to 42
                "name" to "New Item"
            }
        }
        assertEquals(201, response.statusCode)
        assertEquals("/items/42", response.headers["Location"])
        assertEquals("application/json", response.headers["Content-Type"])
        assertEquals("""{"id":42,"name":"New Item"}""", response.body)
    }

    @Test
    fun `respond with body sets raw body`() {
        val ctx = ctx()
        val response = ctx.respond(200) {
            header("Content-Type", "text/plain")
            body("hello world")
        }
        assertEquals("hello world", response.body)
        assertEquals("text/plain", response.headers["Content-Type"])
    }
}
