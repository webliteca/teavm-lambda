package ca.weblite.teavmlambda.demo;

import ca.weblite.teavmlambda.api.Container;
import ca.weblite.teavmlambda.api.Platform;
import ca.weblite.teavmlambda.api.Router;
import ca.weblite.teavmlambda.api.db.Database;
import ca.weblite.teavmlambda.api.db.DatabaseFactory;
import ca.weblite.teavmlambda.generated.GeneratedContainer;
import ca.weblite.teavmlambda.generated.GeneratedRouter;

/**
 * Application entry point.
 * <p>
 * Uses compile-time dependency injection. The {@link GeneratedContainer} is
 * produced by the annotation processor from {@code @Component}, {@code @Service},
 * and {@code @Repository} annotations. External dependencies (like {@link Database})
 * are registered manually before the generated wiring takes effect.
 */
public class Main {

    public static void main(String[] args) {
        String dbUrl = Platform.env("DATABASE_URL", "postgresql://demo:demo@localhost:5432/demo");

        // Create container and register external dependencies
        Container container = new GeneratedContainer();
        container.register(Database.class, DatabaseFactory.create(dbUrl));

        Router router = new GeneratedRouter(container);
        Platform.start(router);
    }
}
