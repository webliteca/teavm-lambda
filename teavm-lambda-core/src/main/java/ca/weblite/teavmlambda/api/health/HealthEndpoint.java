package ca.weblite.teavmlambda.api.health;

import ca.weblite.teavmlambda.api.Middleware;
import ca.weblite.teavmlambda.api.MiddlewareChain;
import ca.weblite.teavmlambda.api.Request;
import ca.weblite.teavmlambda.api.Response;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Health check middleware that intercepts {@code GET /health} and
 * {@code GET /health/ready} requests.
 * <p>
 * Aggregates multiple {@link HealthCheck} components and reports overall status.
 * Returns 200 if all checks pass, 503 if any check fails.
 * <p>
 * Usage:
 * <pre>
 * HealthEndpoint health = new HealthEndpoint()
 *     .add("database", () -&gt; {
 *         try {
 *             db.query("SELECT 1");
 *             return HealthResult.up();
 *         } catch (Exception e) {
 *             return HealthResult.down(e);
 *         }
 *     });
 * </pre>
 */
public final class HealthEndpoint implements Middleware {

    private final Map<String, HealthCheck> checks = new LinkedHashMap<>();

    /**
     * Registers a named health check.
     *
     * @param name  a short name for the check (e.g. "database", "redis")
     * @param check the health check implementation
     * @return this endpoint for fluent chaining
     */
    public HealthEndpoint add(String name, HealthCheck check) {
        checks.put(name, check);
        return this;
    }

    @Override
    public Response handle(Request request, MiddlewareChain chain) {
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            String path = request.getPath();
            if ("/health".equals(path) || "/health/ready".equals(path) || "/health/live".equals(path)) {
                return performChecks();
            }
        }
        return chain.next(request);
    }

    private Response performChecks() {
        Map<String, HealthResult> results = new LinkedHashMap<>();
        boolean allUp = true;

        for (Map.Entry<String, HealthCheck> entry : checks.entrySet()) {
            try {
                HealthResult result = entry.getValue().check();
                results.put(entry.getKey(), result);
                if (!result.isUp()) {
                    allUp = false;
                }
            } catch (Exception e) {
                results.put(entry.getKey(), HealthResult.down(e));
                allUp = false;
            }
        }

        StringBuilder json = new StringBuilder();
        json.append("{\"status\":\"").append(allUp ? "UP" : "DOWN").append('"');

        if (!results.isEmpty()) {
            json.append(",\"checks\":{");
            boolean first = true;
            for (Map.Entry<String, HealthResult> entry : results.entrySet()) {
                if (!first) json.append(',');
                json.append('"').append(entry.getKey()).append("\":");
                json.append(entry.getValue().toJson());
                first = false;
            }
            json.append('}');
        }
        json.append('}');

        int status = allUp ? 200 : 503;
        return Response.status(status)
                .header("Content-Type", "application/json")
                .body(json.toString());
    }
}
