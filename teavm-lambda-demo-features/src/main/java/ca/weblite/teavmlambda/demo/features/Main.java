package ca.weblite.teavmlambda.demo.features;

import ca.weblite.teavmlambda.api.Container;
import ca.weblite.teavmlambda.api.MiddlewareRouter;
import ca.weblite.teavmlambda.api.Platform;
import ca.weblite.teavmlambda.api.Router;
import ca.weblite.teavmlambda.api.db.Database;
import ca.weblite.teavmlambda.api.db.DatabaseFactory;
import ca.weblite.teavmlambda.api.health.HealthEndpoint;
import ca.weblite.teavmlambda.api.health.HealthResult;
import ca.weblite.teavmlambda.api.middleware.CorsMiddleware;
import ca.weblite.teavmlambda.generated.GeneratedContainer;
import ca.weblite.teavmlambda.generated.GeneratedRouter;

/**
 * Features demo entry point.
 * <p>
 * Demonstrates: middleware pipeline, CORS, health checks, compression,
 * validation, PATCH method, @HeaderParam, structured errors, and graceful shutdown.
 */
public class Main {

    public static void main(String[] args) {
        String dbUrl = Platform.env("DATABASE_URL", "postgresql://demo:demo@localhost:5432/demo");

        // Create container and register external dependencies
        Container container = new GeneratedContainer();
        Database db = DatabaseFactory.create(dbUrl);
        container.register(Database.class, db);

        // Health checks with database probe
        HealthEndpoint health = new HealthEndpoint()
                .add("database", () -> {
                    try {
                        db.query("SELECT 1");
                        return HealthResult.up();
                    } catch (Exception e) {
                        return HealthResult.down(e);
                    }
                });

        // Build middleware-wrapped router
        Router router = new MiddlewareRouter(new GeneratedRouter(container))
                .use(health)
                .use(CorsMiddleware.builder()
                        .allowOrigin("*")
                        .allowMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                        .allowHeaders("Content-Type", "Authorization", "X-Request-Id")
                        .build());

        Platform.start(router);
    }
}
