package ca.weblite.teavmlambda.dsl

import ca.weblite.teavmlambda.api.Container
import ca.weblite.teavmlambda.api.Middleware
import ca.weblite.teavmlambda.api.MiddlewareChain
import ca.weblite.teavmlambda.api.MiddlewareRouter
import ca.weblite.teavmlambda.api.Platform
import ca.weblite.teavmlambda.api.Request
import ca.weblite.teavmlambda.api.Response
import ca.weblite.teavmlambda.api.Router
import ca.weblite.teavmlambda.api.middleware.CorsMiddleware

/**
 * Entry point for the Kotlin DSL. Configures middleware, DI services,
 * and routes, then starts the platform.
 *
 * ```kotlin
 * fun main() = app {
 *     cors()
 *     healthCheck("/health")
 *
 *     services {
 *         singleton<ItemService>()
 *         singleton<ItemRepository>()
 *         bind(Database::class.java) { JdbcDatabase(env("DATABASE_URL")) }
 *     }
 *
 *     routes {
 *         "/items" {
 *             get { ok(service<ItemService>().list()) }
 *         }
 *     }
 * }
 * ```
 */
@RoutingDslMarker
class AppScope {

    internal val container = Container()
    internal val middlewares = mutableListOf<Middleware>()
    internal var routingScope: RoutingScope? = null

    // ── Environment ──────────────────────────────────────────────────

    /** Read an environment variable. */
    fun env(name: String): String = Platform.env(name)

    /** Read an environment variable with a default. */
    fun env(name: String, default: String): String = Platform.env(name, default)

    // ── Middleware ────────────────────────────────────────────────────

    /**
     * Add CORS middleware with permissive defaults.
     * Customize via the builder block.
     *
     * ```kotlin
     * cors {
     *     allowOrigin("https://example.com")
     *     allowCredentials(true)
     * }
     * ```
     */
    fun cors(block: CorsMiddleware.Builder.() -> Unit = {}) {
        val builder = CorsMiddleware.builder()
        builder.block()
        middlewares += builder.build()
    }

    /**
     * Add a health check endpoint that returns 200 OK.
     *
     * ```kotlin
     * healthCheck("/health")
     * ```
     */
    fun healthCheck(path: String = "/health", body: String = """{"status":"ok"}""") {
        middlewares += Middleware { request, chain ->
            if (request.path == path && (request.method == "GET" || request.method == "HEAD")) {
                Response.ok(body).header("Content-Type", "application/json")
            } else {
                chain.next(request)
            }
        }
    }

    /**
     * Add a custom middleware using a lambda.
     *
     * ```kotlin
     * middleware { request, next ->
     *     val start = System.currentTimeMillis()
     *     val response = next(request)
     *     response.header("X-Duration", "${System.currentTimeMillis() - start}ms")
     * }
     * ```
     */
    fun middleware(handler: (request: Request, next: (Request) -> Response) -> Response) {
        middlewares += Middleware { request, chain ->
            handler(request) { req -> chain.next(req) }
        }
    }

    /** Add a pre-built Java [Middleware] instance. */
    fun use(middleware: Middleware) {
        middlewares += middleware
    }

    // ── Dependency Injection ─────────────────────────────────────────

    /**
     * Configure DI services.
     *
     * ```kotlin
     * services {
     *     bind(Database::class.java) { JdbcDatabase(env("DATABASE_URL")) }
     *     singleton<ItemService> { ItemService(get()) }
     *     instance(myConfigObject)
     * }
     * ```
     */
    fun services(block: ServiceScope.() -> Unit) {
        val scope = ServiceScope(container, this)
        scope.block()
    }

    // ── Routes ───────────────────────────────────────────────────────

    /**
     * Define routes using the Kotlin routing DSL.
     *
     * ```kotlin
     * routes {
     *     "/items" {
     *         get { ok(service<ItemService>().list()) }
     *         "/{id}" {
     *             get { ok(service<ItemService>().find(pathInt("id")) ?: throw NotFound()) }
     *         }
     *     }
     * }
     * ```
     */
    fun routes(block: RoutingScope.() -> Unit) {
        val scope = RoutingScope()
        scope.block()
        routingScope = scope
    }

    // ── Build ────────────────────────────────────────────────────────

    internal fun build(): Router {
        val rs = routingScope ?: throw IllegalStateException("No routes defined. Call routes { } in your app block.")
        val dslRouter = DslRouter.compile(rs.routes, container)

        return if (middlewares.isEmpty()) {
            dslRouter
        } else {
            val mw = MiddlewareRouter(dslRouter)
            for (m in middlewares) mw.use(m)
            mw
        }
    }
}

/**
 * DSL scope for registering services in the DI container.
 */
@RoutingDslMarker
class ServiceScope(
    @PublishedApi internal val container: Container,
    private val appScope: AppScope
) {

    /** Read an environment variable (convenience delegation). */
    fun env(name: String): String = appScope.env(name)

    /** Read an environment variable with a default. */
    fun env(name: String, default: String): String = appScope.env(name, default)

    /** Register a factory for the given type. */
    fun <T> bind(type: Class<T>, factory: Container.Factory<T>) {
        container.register(type, factory)
    }

    /** Register a factory using a lambda. */
    fun <T> bind(type: Class<T>, factory: () -> T) {
        container.register(type, Container.Factory { factory() })
    }

    /** Register an existing instance (singleton). */
    fun <T> instance(type: Class<T>, instance: T) {
        container.register(type, instance)
    }

    /** Register an existing instance with reified type. */
    inline fun <reified T> instance(instance: T) {
        container.register(T::class.java, instance)
    }

    /** Register a singleton factory for the given type. */
    fun <T> singleton(type: Class<T>, factory: () -> T) {
        container.registerSingleton(type, Container.Factory { factory() })
    }

    /** Register a singleton factory with reified type. */
    inline fun <reified T> singleton(noinline factory: () -> T) {
        container.registerSingleton(T::class.java, Container.Factory { factory() })
    }

    /** Resolve a service from the container (for wiring dependencies). */
    fun <T> get(type: Class<T>): T = container.get(type)

    /** Resolve a service with reified type. */
    inline fun <reified T> get(): T = container.get(T::class.java)
}

/**
 * Top-level application entry point.
 *
 * ```kotlin
 * fun main() = app {
 *     cors()
 *     routes {
 *         "/hello" {
 *             get { ok("Hello, Kotlin!") }
 *         }
 *     }
 * }
 * ```
 */
fun app(block: AppScope.() -> Unit) {
    val scope = AppScope()
    scope.block()
    val router = scope.build()
    Platform.start(router)
}

/**
 * Build a router from the DSL without starting the platform.
 * Useful for testing or embedding in servlet containers.
 *
 * ```kotlin
 * val router = router {
 *     routes {
 *         "/test" { get { ok("works") } }
 *     }
 * }
 * ```
 */
fun router(block: AppScope.() -> Unit): Router {
    val scope = AppScope()
    scope.block()
    return scope.build()
}
