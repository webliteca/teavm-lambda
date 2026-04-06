package ca.weblite.teavmlambda.demo.features.lambda;

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

public class Main {

    public static void main(String[] args) {
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

        Router router = new MiddlewareRouter(new GeneratedRouter(container))
                .use(health)
                .use(CorsMiddleware.builder().allowOrigin("*").build());

        Platform.start(router);
    }
}
