package ca.weblite.teavmlambda.dsl

import ca.weblite.teavmlambda.api.Container
import ca.weblite.teavmlambda.api.Request
import ca.weblite.teavmlambda.api.Response

/**
 * DSL for defining routes with nested path segments and HTTP method handlers.
 *
 * ```kotlin
 * routes {
 *     "/items" {
 *         get { ok(service<ItemService>().list()) }
 *         post { created(service<ItemService>().create(body(CreateItemRequest))) }
 *
 *         "/{id}" {
 *             get { ok(service<ItemService>().find(pathInt("id")) ?: throw NotFound()) }
 *             delete {
 *                 service<ItemService>().delete(pathInt("id"))
 *                 noContent()
 *             }
 *         }
 *     }
 * }
 * ```
 */
@DslMarker
annotation class RoutingDslMarker

/** A route handler: a function on [RequestContext] returning a [Response]. */
typealias RouteHandler = RequestContext.() -> Response

/** Internal representation of a registered route. */
internal data class Route(
    val method: String,
    val pathPattern: String,
    val handler: RouteHandler
)

/**
 * Top-level routing scope. Collects route definitions and compiles them
 * into a [DslRouter] that implements [ca.weblite.teavmlambda.api.Router].
 */
@RoutingDslMarker
class RoutingScope(private val prefix: String = "") {

    @PublishedApi
    internal val routes = mutableListOf<Route>()

    /** Define a nested path group. */
    operator fun String.invoke(block: RoutingScope.() -> Unit) {
        val nested = RoutingScope(prefix + this)
        nested.block()
        routes.addAll(nested.routes)
    }

    /** Register a GET handler at the current path. */
    fun get(handler: RouteHandler) {
        routes += Route("GET", prefix, handler)
    }

    /** Register a GET handler at a sub-path. */
    fun get(path: String, handler: RouteHandler) {
        routes += Route("GET", prefix + path, handler)
    }

    /** Register a POST handler at the current path. */
    fun post(handler: RouteHandler) {
        routes += Route("POST", prefix, handler)
    }

    /** Register a POST handler at a sub-path. */
    fun post(path: String, handler: RouteHandler) {
        routes += Route("POST", prefix + path, handler)
    }

    /** Register a PUT handler at the current path. */
    fun put(handler: RouteHandler) {
        routes += Route("PUT", prefix, handler)
    }

    /** Register a PUT handler at a sub-path. */
    fun put(path: String, handler: RouteHandler) {
        routes += Route("PUT", prefix + path, handler)
    }

    /** Register a PATCH handler at the current path. */
    fun patch(handler: RouteHandler) {
        routes += Route("PATCH", prefix, handler)
    }

    /** Register a PATCH handler at a sub-path. */
    fun patch(path: String, handler: RouteHandler) {
        routes += Route("PATCH", prefix + path, handler)
    }

    /** Register a DELETE handler at the current path. */
    fun delete(handler: RouteHandler) {
        routes += Route("DELETE", prefix, handler)
    }

    /** Register a DELETE handler at a sub-path. */
    fun delete(path: String, handler: RouteHandler) {
        routes += Route("DELETE", prefix + path, handler)
    }

    /** Register a HEAD handler at the current path. */
    fun head(handler: RouteHandler) {
        routes += Route("HEAD", prefix, handler)
    }

    /** Register an OPTIONS handler at the current path. */
    fun options(handler: RouteHandler) {
        routes += Route("OPTIONS", prefix, handler)
    }
}

/**
 * A compiled route entry with pre-parsed path segments for fast matching.
 */
internal class CompiledRoute(
    val method: String,
    val segments: List<PathSegment>,
    val handler: RouteHandler
)

internal sealed class PathSegment {
    data class Literal(val value: String) : PathSegment()
    data class Param(val name: String) : PathSegment()
}

/**
 * Router implementation built from the Kotlin DSL.
 * Matches requests against registered routes, extracts path params,
 * and catches [HttpException] to produce ProblemDetail responses.
 */
class DslRouter internal constructor(
    private val routes: List<CompiledRoute>,
    private val container: Container
) : ca.weblite.teavmlambda.api.Router {

    override fun route(request: Request): Response {
        val requestSegments = request.path.trimEnd('/').split('/').filter { it.isNotEmpty() }
        val method = request.method.uppercase()

        var methodMatched = false

        for (route in routes) {
            if (route.method != method && route.method != "*") {
                // Check if the path matches even if method doesn't (for 405)
                val pathMatch = matchPath(route.segments, requestSegments)
                if (pathMatch != null) methodMatched = true
                continue
            }

            val pathParams = matchPath(route.segments, requestSegments) ?: continue

            val enrichedRequest = if (pathParams.isEmpty()) request else request.withPathParams(
                buildMap {
                    putAll(request.pathParams)
                    putAll(pathParams)
                }
            )

            val ctx = RequestContext(enrichedRequest, container)
            return try {
                ctx.(route.handler)()
            } catch (e: ValidationException) {
                e.toValidationResponse()
            } catch (e: HttpException) {
                e.toResponse()
            }
        }

        if (methodMatched) {
            return ca.weblite.teavmlambda.api.ProblemDetail.of(405, "Method Not Allowed").toResponse()
        }

        return ca.weblite.teavmlambda.api.ProblemDetail.notFound(
            "No route found for ${request.method} ${request.path}"
        ).toResponse()
    }

    private fun matchPath(
        segments: List<PathSegment>,
        requestSegments: List<String>
    ): Map<String, String>? {
        if (segments.size != requestSegments.size) return null

        val params = mutableMapOf<String, String>()
        for (i in segments.indices) {
            when (val seg = segments[i]) {
                is PathSegment.Literal -> {
                    if (!seg.value.equals(requestSegments[i], ignoreCase = true)) return null
                }
                is PathSegment.Param -> {
                    params[seg.name] = requestSegments[i]
                }
            }
        }
        return params
    }

    companion object {
        /** Compile a [RoutingScope] into a [DslRouter]. */
        internal fun compile(routes: List<Route>, container: Container): DslRouter {
            val compiled = routes.map { route ->
                val segments = route.pathPattern
                    .trimEnd('/')
                    .split('/')
                    .filter { it.isNotEmpty() }
                    .map { seg ->
                        if (seg.startsWith("{") && seg.endsWith("}")) {
                            PathSegment.Param(seg.substring(1, seg.length - 1))
                        } else {
                            PathSegment.Literal(seg)
                        }
                    }
                CompiledRoute(route.method, segments, route.handler)
            }
            return DslRouter(compiled, container)
        }
    }
}
