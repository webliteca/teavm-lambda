package ca.weblite.teavmlambda.api;

import java.util.List;

/**
 * Executes a chain of {@link Middleware} components followed by a terminal {@link Router}.
 * <p>
 * Each call to {@link #next(Request)} advances to the next middleware in the chain.
 * When all middleware have been executed, the request is passed to the terminal router.
 */
public final class MiddlewareChain {

    private final List<Middleware> middlewares;
    private final Router terminal;
    private final int index;

    MiddlewareChain(List<Middleware> middlewares, Router terminal) {
        this(middlewares, terminal, 0);
    }

    private MiddlewareChain(List<Middleware> middlewares, Router terminal, int index) {
        this.middlewares = middlewares;
        this.terminal = terminal;
        this.index = index;
    }

    /**
     * Passes the request to the next middleware, or to the terminal router
     * if all middleware have been executed.
     *
     * @param request the request to process
     * @return the response
     */
    public Response next(Request request) {
        if (index < middlewares.size()) {
            Middleware middleware = middlewares.get(index);
            MiddlewareChain nextChain = new MiddlewareChain(middlewares, terminal, index + 1);
            return middleware.handle(request, nextChain);
        }
        return terminal.route(request);
    }
}
