package ca.weblite.teavmlambda.api;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link Router} that wraps another router with a chain of {@link Middleware}.
 * <p>
 * Usage:
 * <pre>
 * Router router = new MiddlewareRouter(new GeneratedRouter(container))
 *     .use(new CorsMiddleware(...))
 *     .use(new CompressionMiddleware());
 * Platform.start(router);
 * </pre>
 */
public final class MiddlewareRouter implements Router {

    private final Router delegate;
    private final List<Middleware> middlewares = new ArrayList<>();

    /**
     * Creates a middleware router wrapping the given delegate router.
     *
     * @param delegate the terminal router that handles requests after all middleware
     */
    public MiddlewareRouter(Router delegate) {
        this.delegate = delegate;
    }

    /**
     * Appends a middleware to the chain.
     *
     * @param middleware the middleware to add
     * @return this router for fluent chaining
     */
    public MiddlewareRouter use(Middleware middleware) {
        middlewares.add(middleware);
        return this;
    }

    @Override
    public Response route(Request request) {
        if (middlewares.isEmpty()) {
            return delegate.route(request);
        }
        MiddlewareChain chain = new MiddlewareChain(middlewares, delegate);
        return chain.next(request);
    }
}
