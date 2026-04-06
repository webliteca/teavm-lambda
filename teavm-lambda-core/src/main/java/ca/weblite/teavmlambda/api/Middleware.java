package ca.weblite.teavmlambda.api;

/**
 * A middleware component that can intercept and transform requests and responses.
 * <p>
 * Middleware forms a chain: each middleware can inspect/modify the request,
 * delegate to the next middleware (or the terminal router) via
 * {@link MiddlewareChain#next(Request)}, and inspect/modify the response.
 * <p>
 * Example:
 * <pre>
 * public class LoggingMiddleware implements Middleware {
 *     public Response handle(Request request, MiddlewareChain chain) {
 *         long start = System.currentTimeMillis();
 *         Response response = chain.next(request);
 *         long duration = System.currentTimeMillis() - start;
 *         System.out.println(request.getMethod() + " " + request.getPath() + " -> " + response.getStatusCode() + " (" + duration + "ms)");
 *         return response;
 *     }
 * }
 * </pre>
 */
@FunctionalInterface
public interface Middleware {

    /**
     * Handles the request, optionally delegating to the next middleware in the chain.
     *
     * @param request the incoming request
     * @param chain   the remaining middleware chain; call {@code chain.next(request)} to continue
     * @return the response
     */
    Response handle(Request request, MiddlewareChain chain);
}
