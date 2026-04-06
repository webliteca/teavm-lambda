package ca.weblite.teavmlambda.dsl

import ca.weblite.teavmlambda.api.ProblemDetail
import ca.weblite.teavmlambda.api.Response

/**
 * Sealed hierarchy of HTTP exceptions that automatically convert to
 * RFC 7807 ProblemDetail responses when thrown from route handlers.
 *
 * Usage:
 * ```kotlin
 * get("/{id}") {
 *     val item = service.find(path("id")) ?: throw NotFound("Item not found")
 *     ok(item)
 * }
 * ```
 */
sealed class HttpException(
    val status: Int,
    override val message: String
) : RuntimeException(message) {

    /** Converts this exception to an RFC 7807 ProblemDetail. */
    fun toProblemDetail(): ProblemDetail = ProblemDetail.of(status, message)

    /** Converts this exception to a Response with application/problem+json content type. */
    fun toResponse(): Response = toProblemDetail().toResponse()
}

/** 400 Bad Request */
class BadRequest(message: String = "Bad Request") : HttpException(400, message)

/** 401 Unauthorized */
class Unauthorized(message: String = "Unauthorized") : HttpException(401, message)

/** 403 Forbidden */
class Forbidden(message: String = "Forbidden") : HttpException(403, message)

/** 404 Not Found */
class NotFound(message: String = "Not Found") : HttpException(404, message)

/** 405 Method Not Allowed */
class MethodNotAllowed(message: String = "Method Not Allowed") : HttpException(405, message)

/** 409 Conflict */
class Conflict(message: String = "Conflict") : HttpException(409, message)

/** 422 Unprocessable Entity */
class UnprocessableEntity(message: String = "Unprocessable Entity") : HttpException(422, message)

/** 429 Too Many Requests */
class TooManyRequests(message: String = "Too Many Requests") : HttpException(429, message)

/** 500 Internal Server Error */
class InternalError(message: String = "Internal Server Error") : HttpException(500, message)
