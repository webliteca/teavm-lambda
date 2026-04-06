package ca.weblite.teavmlambda.dsl

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class HttpExceptionTest {

    @Test
    fun `BadRequest has status 400`() {
        val ex = BadRequest("invalid input")
        assertEquals(400, ex.status)
        assertEquals("invalid input", ex.message)
    }

    @Test
    fun `Unauthorized has status 401`() {
        val ex = Unauthorized()
        assertEquals(401, ex.status)
        assertEquals("Unauthorized", ex.message)
    }

    @Test
    fun `Forbidden has status 403`() {
        assertEquals(403, Forbidden().status)
    }

    @Test
    fun `NotFound has status 404`() {
        val ex = NotFound("Item not found")
        assertEquals(404, ex.status)
        assertEquals("Item not found", ex.message)
    }

    @Test
    fun `MethodNotAllowed has status 405`() {
        assertEquals(405, MethodNotAllowed().status)
    }

    @Test
    fun `Conflict has status 409`() {
        assertEquals(409, Conflict().status)
    }

    @Test
    fun `UnprocessableEntity has status 422`() {
        assertEquals(422, UnprocessableEntity().status)
    }

    @Test
    fun `TooManyRequests has status 429`() {
        assertEquals(429, TooManyRequests().status)
    }

    @Test
    fun `InternalError has status 500`() {
        assertEquals(500, InternalError().status)
    }

    @Test
    fun `all HttpExceptions are subtypes of RuntimeException`() {
        val exceptions: List<HttpException> = listOf(
            BadRequest(), Unauthorized(), Forbidden(), NotFound(),
            MethodNotAllowed(), Conflict(), UnprocessableEntity(),
            TooManyRequests(), InternalError()
        )
        for (ex in exceptions) {
            assertIs<RuntimeException>(ex)
        }
    }

    @Test
    fun `toProblemDetail creates correct ProblemDetail`() {
        val pd = NotFound("Item 42 not found").toProblemDetail()
        assertEquals(404, pd.status)
        assertEquals("Item 42 not found", pd.detail)
        assertEquals("Not Found", pd.title)
    }

    @Test
    fun `toResponse creates correct Response`() {
        val response = BadRequest("bad input").toResponse()
        assertEquals(400, response.statusCode)
        assertEquals("application/problem+json", response.headers["Content-Type"])
        assertTrue(response.body.contains("bad input"))
    }

    @Test
    fun `default messages are set correctly`() {
        assertEquals("Bad Request", BadRequest().message)
        assertEquals("Not Found", NotFound().message)
        assertEquals("Internal Server Error", InternalError().message)
    }
}
