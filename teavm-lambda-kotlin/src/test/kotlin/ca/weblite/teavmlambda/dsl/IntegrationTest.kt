package ca.weblite.teavmlambda.dsl

import ca.weblite.teavmlambda.api.Request
import ca.weblite.teavmlambda.api.Router
import ca.weblite.teavmlambda.api.json.JsonReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests that exercise the full Kotlin DSL stack:
 * router builder, services, middleware, routing, request context,
 * JSON DSL, validation, and error handling — all wired together.
 */
class IntegrationTest {

    // ── Test fixtures ────────────────────────────────────────────────

    data class Item(val id: Int, val name: String, val quantity: Int) : JsonSerializable {
        override fun toJson() = json {
            "id" to id
            "name" to name
            "quantity" to quantity
        }

        companion object : JsonDeserializer<Item> {
            override fun fromJson(json: String): Item {
                val r = JsonReader.parse(json)
                return Item(r.getInt("id", 0), r.getString("name"), r.getInt("quantity", 0))
            }
        }
    }

    class ItemService {
        private val items = mutableListOf(
            Item(1, "Widget", 10),
            Item(2, "Gadget", 5)
        )
        private var nextId = 3

        fun list(): List<Item> = items.toList()
        fun find(id: Int): Item? = items.find { it.id == id }
        fun create(name: String, quantity: Int): Item {
            val item = Item(nextId++, name, quantity)
            items.add(item)
            return item
        }
        fun delete(id: Int): Boolean = items.removeIf { it.id == id }
    }

    private fun buildApp(): Router = router {
        services {
            singleton<ItemService> { ItemService() }
        }

        healthCheck("/health")

        middleware { request, next ->
            val response = next(request)
            response.header("X-Powered-By", "teavm-lambda-kt")
        }

        routes {
            "/items" {
                get {
                    val items = service<ItemService>().list()
                    ok(items.map { it as JsonSerializable })
                }

                post {
                    val req = body(Item)
                    validate {
                        notEmpty(req.name, "name")
                        min(req.quantity, 0, "quantity")
                        max(req.quantity, 10_000, "quantity")
                    }
                    val item = service<ItemService>().create(req.name, req.quantity)
                    created(item)
                }

                "/{id}" {
                    get {
                        val item = service<ItemService>().find(pathInt("id"))
                            ?: throw NotFound("Item not found")
                        ok(item)
                    }

                    delete {
                        val deleted = service<ItemService>().delete(pathInt("id"))
                        if (!deleted) throw NotFound("Item not found")
                        noContent()
                    }
                }
            }

            "/echo" {
                post {
                    respond(200) {
                        header("X-Echo", "true")
                        body(requireBody())
                    }
                }
            }
        }
    }

    private fun request(
        method: String,
        path: String,
        body: String? = null,
        headers: Map<String, String> = emptyMap(),
        queryParams: Map<String, String> = emptyMap()
    ) = Request(method, path, headers, queryParams, body)

    // ── Integration tests ────────────────────────────────────────────

    @Test
    fun `GET health check returns 200`() {
        val router = buildApp()
        val response = router.route(request("GET", "/health"))
        assertEquals(200, response.statusCode)
        assertEquals("""{"status":"ok"}""", response.body)
    }

    @Test
    fun `middleware adds X-Powered-By header to all responses`() {
        val router = buildApp()
        val response = router.route(request("GET", "/items"))
        assertEquals("teavm-lambda-kt", response.headers["X-Powered-By"])
    }

    @Test
    fun `GET items returns list`() {
        val router = buildApp()
        val response = router.route(request("GET", "/items"))
        assertEquals(200, response.statusCode)
        assertEquals("application/json", response.headers["Content-Type"])
        assertTrue(response.body.contains("Widget"))
        assertTrue(response.body.contains("Gadget"))
    }

    @Test
    fun `GET items by id returns single item`() {
        val router = buildApp()
        val response = router.route(request("GET", "/items/1"))
        assertEquals(200, response.statusCode)
        val reader = JsonReader.parse(response.body)
        assertEquals("Widget", reader.getString("name"))
        assertEquals(10, reader.getInt("quantity", 0))
    }

    @Test
    fun `GET items with invalid id returns 404`() {
        val router = buildApp()
        val response = router.route(request("GET", "/items/999"))
        assertEquals(404, response.statusCode)
        assertTrue(response.body.contains("Item not found"))
    }

    @Test
    fun `GET items with non-numeric id returns 400`() {
        val router = buildApp()
        val response = router.route(request("GET", "/items/abc"))
        assertEquals(400, response.statusCode)
    }

    @Test
    fun `POST items creates new item`() {
        val router = buildApp()
        val response = router.route(request(
            "POST", "/items",
            body = """{"name":"Sprocket","quantity":3}"""
        ))
        assertEquals(201, response.statusCode)
        val reader = JsonReader.parse(response.body)
        assertEquals("Sprocket", reader.getString("name"))
        assertEquals(3, reader.getInt("quantity", 0))
        assertTrue(reader.getInt("id", 0) > 0)
    }

    @Test
    fun `POST items with empty name returns validation error`() {
        val router = buildApp()
        val response = router.route(request(
            "POST", "/items",
            body = """{"name":"","quantity":5}"""
        ))
        assertEquals(400, response.statusCode)
        assertTrue(response.body.contains(""""field":"name""""))
    }

    @Test
    fun `POST items with negative quantity returns validation error`() {
        val router = buildApp()
        val response = router.route(request(
            "POST", "/items",
            body = """{"name":"Test","quantity":-1}"""
        ))
        assertEquals(400, response.statusCode)
        assertTrue(response.body.contains(""""field":"quantity""""))
    }

    @Test
    fun `POST items with quantity above max returns validation error`() {
        val router = buildApp()
        val response = router.route(request(
            "POST", "/items",
            body = """{"name":"Test","quantity":99999}"""
        ))
        assertEquals(400, response.statusCode)
        assertTrue(response.body.contains("quantity"))
    }

    @Test
    fun `DELETE items removes item`() {
        val router = buildApp()
        val response = router.route(request("DELETE", "/items/1"))
        assertEquals(204, response.statusCode)

        // Verify it's gone
        val getResponse = router.route(request("GET", "/items/1"))
        assertEquals(404, getResponse.statusCode)
    }

    @Test
    fun `DELETE non-existent item returns 404`() {
        val router = buildApp()
        val response = router.route(request("DELETE", "/items/999"))
        assertEquals(404, response.statusCode)
    }

    @Test
    fun `wrong method returns 405`() {
        val router = buildApp()
        val response = router.route(request("PUT", "/items"))
        assertEquals(405, response.statusCode)
    }

    @Test
    fun `unknown path returns 404`() {
        val router = buildApp()
        val response = router.route(request("GET", "/not-a-route"))
        assertEquals(404, response.statusCode)
    }

    @Test
    fun `echo endpoint uses respond DSL`() {
        val router = buildApp()
        val response = router.route(request("POST", "/echo", body = "hello world"))
        assertEquals(200, response.statusCode)
        assertEquals("true", response.headers["X-Echo"])
        assertEquals("hello world", response.body)
    }

    @Test
    fun `services are singletons across requests`() {
        val router = buildApp()

        // Create an item
        router.route(request("POST", "/items", body = """{"name":"New","quantity":1}"""))

        // List should include the new item (proving same service instance)
        val response = router.route(request("GET", "/items"))
        assertTrue(response.body.contains("New"))
    }

    @Test
    fun `health check does not pass through to routes`() {
        val router = buildApp()
        val response = router.route(request("GET", "/health"))
        assertEquals(200, response.statusCode)
        // Health check middleware should short-circuit — no route matching needed
        assertEquals("""{"status":"ok"}""", response.body)
    }

    @Test
    fun `POST with no body returns 400`() {
        val router = buildApp()
        val response = router.route(request("POST", "/items"))
        assertEquals(400, response.statusCode)
    }

    // ── Multiple middleware ordering ─────────────────────────────────

    @Test
    fun `middleware executes in order`() {
        val router = router {
            middleware { request, next ->
                next(request).header("X-First", "1")
            }
            middleware { request, next ->
                next(request).header("X-Second", "2")
            }
            routes {
                get("/test") { ok("test") }
            }
        }
        val response = router.route(request("GET", "/test"))
        assertEquals("1", response.headers["X-First"])
        assertEquals("2", response.headers["X-Second"])
    }
}
