package ca.weblite.teavmlambda.dsl

import ca.weblite.teavmlambda.api.Container
import ca.weblite.teavmlambda.api.Request
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RoutingTest {

    private fun get(path: String, body: String? = null) =
        Request("GET", path, emptyMap(), emptyMap(), body)

    private fun post(path: String, body: String? = null) =
        Request("POST", path, emptyMap(), emptyMap(), body)

    private fun put(path: String, body: String? = null) =
        Request("PUT", path, emptyMap(), emptyMap(), body)

    private fun delete(path: String) =
        Request("DELETE", path, emptyMap(), emptyMap(), null as String?)

    private fun patch(path: String, body: String? = null) =
        Request("PATCH", path, emptyMap(), emptyMap(), body)

    private fun buildRouter(block: RoutingScope.() -> Unit): DslRouter {
        val scope = RoutingScope()
        scope.block()
        return DslRouter.compile(scope.routes, Container())
    }

    // ── Basic routing ────────────────────────────────────────────────

    @Test
    fun `routes simple GET`() {
        val router = buildRouter {
            get("/hello") { ok("Hello!") }
        }
        val response = router.route(get("/hello"))
        assertEquals(200, response.statusCode)
        assertEquals("Hello!", response.body)
    }

    @Test
    fun `routes simple POST`() {
        val router = buildRouter {
            post("/items") { created("""{"id":1}""") }
        }
        val response = router.route(post("/items"))
        assertEquals(201, response.statusCode)
    }

    @Test
    fun `routes PUT`() {
        val router = buildRouter {
            put("/items") { ok("updated") }
        }
        assertEquals(200, router.route(put("/items")).statusCode)
    }

    @Test
    fun `routes DELETE`() {
        val router = buildRouter {
            delete("/items") { noContent() }
        }
        assertEquals(204, router.route(delete("/items")).statusCode)
    }

    @Test
    fun `routes PATCH`() {
        val router = buildRouter {
            patch("/items") { ok("patched") }
        }
        assertEquals(200, router.route(patch("/items")).statusCode)
    }

    // ── Path parameters ──────────────────────────────────────────────

    @Test
    fun `extracts path parameters`() {
        val router = buildRouter {
            get("/items/{id}") { ok(path("id")) }
        }
        val response = router.route(get("/items/42"))
        assertEquals(200, response.statusCode)
        assertEquals("42", response.body)
    }

    @Test
    fun `extracts multiple path parameters`() {
        val router = buildRouter {
            get("/users/{userId}/posts/{postId}") {
                ok("""${path("userId")}:${path("postId")}""")
            }
        }
        val response = router.route(get("/users/5/posts/10"))
        assertEquals("5:10", response.body)
    }

    // ── Nested paths ─────────────────────────────────────────────────

    @Test
    fun `nested path groups`() {
        val router = buildRouter {
            "/items" {
                get { ok("list") }
                post { created("new") }
                "/{id}" {
                    get { ok("item ${path("id")}") }
                    delete { noContent() }
                }
            }
        }

        assertEquals("list", router.route(get("/items")).body)
        assertEquals(201, router.route(post("/items")).statusCode)
        assertEquals("item 42", router.route(get("/items/42")).body)
        assertEquals(204, router.route(delete("/items/42")).statusCode)
    }

    @Test
    fun `deeply nested paths`() {
        val router = buildRouter {
            "/api" {
                "/v1" {
                    "/users" {
                        get { ok("users") }
                    }
                }
            }
        }
        assertEquals("users", router.route(get("/api/v1/users")).body)
    }

    // ── Error handling ───────────────────────────────────────────────

    @Test
    fun `returns 404 for unknown path`() {
        val router = buildRouter {
            get("/hello") { ok("hi") }
        }
        val response = router.route(get("/unknown"))
        assertEquals(404, response.statusCode)
        assertEquals("application/problem+json", response.headers["Content-Type"])
    }

    @Test
    fun `returns 405 for wrong method`() {
        val router = buildRouter {
            get("/items") { ok("list") }
        }
        val response = router.route(post("/items"))
        assertEquals(405, response.statusCode)
    }

    @Test
    fun `catches HttpException and returns ProblemDetail`() {
        val router = buildRouter {
            get("/items/{id}") {
                throw NotFound("Item ${path("id")} not found")
            }
        }
        val response = router.route(get("/items/99"))
        assertEquals(404, response.statusCode)
        assertEquals("application/problem+json", response.headers["Content-Type"])
        assertTrue(response.body.contains("Item 99 not found"))
    }

    @Test
    fun `catches ValidationException and returns structured errors`() {
        val router = buildRouter {
            post("/items") {
                validate {
                    notEmpty("", "name")
                }
                ok("should not reach")
            }
        }
        val response = router.route(post("/items"))
        assertEquals(400, response.statusCode)
        assertEquals("application/json", response.headers["Content-Type"])
        assertTrue(response.body.contains(""""field":"name""""))
    }

    // ── Trailing slash handling ───────────────────────────────────────

    @Test
    fun `trailing slash is normalized`() {
        val router = buildRouter {
            get("/items") { ok("list") }
        }
        assertEquals("list", router.route(get("/items/")).body)
    }

    // ── Case insensitive path matching ───────────────────────────────

    @Test
    fun `path matching is case insensitive for literal segments`() {
        val router = buildRouter {
            get("/Items") { ok("found") }
        }
        assertEquals("found", router.route(get("/items")).body)
        assertEquals("found", router.route(get("/ITEMS")).body)
    }

    // ── RoutingScope collects all methods ────────────────────────────

    @Test
    fun `all HTTP methods register correctly`() {
        val scope = RoutingScope()
        scope.apply {
            get("/a") { ok("") }
            post("/b") { ok("") }
            put("/c") { ok("") }
            patch("/d") { ok("") }
            delete("/e") { ok("") }
            "/f" { head { ok("") } }
            "/g" { options { ok("") } }
        }
        val methods = scope.routes.map { it.method }
        assertEquals(listOf("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS"), methods)
    }
}
