package ca.weblite.teavmlambda.demo.features.war;

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
import ca.weblite.teavmlambda.impl.jvm.war.WarServlet;
import jakarta.servlet.annotation.WebServlet;

/**
 * WAR deployment servlet for the features demo.
 * Deploys the same application as {@code Main.java} on a Servlet container.
 */
@WebServlet(urlPatterns = "/*")
public class FeaturesWarServlet extends WarServlet {

    @Override
    protected Router createRouter() {
        String dbUrl = Platform.env("DATABASE_URL", "postgresql://demo:demo@localhost:5432/demo");

        Container container = new GeneratedContainer();
        Database db = DatabaseFactory.create(dbUrl);
        container.register(Database.class, db);

        HealthEndpoint health = new HealthEndpoint()
                .add("database", () -> {
                    try {
                        db.query("SELECT 1");
                        return HealthResult.up();
                    } catch (Exception e) {
                        return HealthResult.down(e);
                    }
                });

        return new MiddlewareRouter(new GeneratedRouter(container))
                .use(health)
                .use(CorsMiddleware.builder()
                        .allowOrigin("*")
                        .allowMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                        .allowHeaders("Content-Type", "Authorization", "X-Request-Id")
                        .build());
    }
}
