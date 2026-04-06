package ca.weblite.teavmlambda.api.middleware;

import ca.weblite.teavmlambda.api.Middleware;
import ca.weblite.teavmlambda.api.MiddlewareChain;
import ca.weblite.teavmlambda.api.Request;
import ca.weblite.teavmlambda.api.Response;

/**
 * CORS (Cross-Origin Resource Sharing) middleware.
 * <p>
 * Handles preflight OPTIONS requests and adds CORS headers to all responses.
 * <p>
 * Usage:
 * <pre>
 * CorsMiddleware cors = CorsMiddleware.builder()
 *     .allowOrigin("https://example.com")
 *     .allowMethods("GET", "POST", "PUT", "DELETE", "PATCH")
 *     .allowHeaders("Content-Type", "Authorization")
 *     .allowCredentials(true)
 *     .maxAge(3600)
 *     .build();
 * </pre>
 */
public final class CorsMiddleware implements Middleware {

    private final String allowOrigin;
    private final String allowMethods;
    private final String allowHeaders;
    private final boolean allowCredentials;
    private final int maxAge;

    private CorsMiddleware(Builder builder) {
        this.allowOrigin = builder.allowOrigin;
        this.allowMethods = builder.allowMethods;
        this.allowHeaders = builder.allowHeaders;
        this.allowCredentials = builder.allowCredentials;
        this.maxAge = builder.maxAge;
    }

    @Override
    public Response handle(Request request, MiddlewareChain chain) {
        // Handle preflight OPTIONS request
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return addCorsHeaders(Response.status(204));
        }

        // Process the request and add CORS headers to the response
        Response response = chain.next(request);
        return addCorsHeaders(response);
    }

    private Response addCorsHeaders(Response response) {
        response = response.header("Access-Control-Allow-Origin", allowOrigin);
        response = response.header("Access-Control-Allow-Methods", allowMethods);
        response = response.header("Access-Control-Allow-Headers", allowHeaders);
        if (allowCredentials) {
            response = response.header("Access-Control-Allow-Credentials", "true");
        }
        if (maxAge > 0) {
            response = response.header("Access-Control-Max-Age", String.valueOf(maxAge));
        }
        return response;
    }

    /**
     * Creates a new builder with permissive defaults (allow all origins).
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String allowOrigin = "*";
        private String allowMethods = "GET, POST, PUT, DELETE, PATCH, OPTIONS, HEAD";
        private String allowHeaders = "Content-Type, Authorization, X-Request-Id";
        private boolean allowCredentials = false;
        private int maxAge = 86400;

        private Builder() {
        }

        public Builder allowOrigin(String origin) {
            this.allowOrigin = origin;
            return this;
        }

        public Builder allowMethods(String... methods) {
            this.allowMethods = String.join(", ", methods);
            return this;
        }

        public Builder allowHeaders(String... headers) {
            this.allowHeaders = String.join(", ", headers);
            return this;
        }

        public Builder allowCredentials(boolean allow) {
            this.allowCredentials = allow;
            return this;
        }

        public Builder maxAge(int seconds) {
            this.maxAge = seconds;
            return this;
        }

        public CorsMiddleware build() {
            return new CorsMiddleware(this);
        }
    }
}
