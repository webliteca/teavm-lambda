package ca.weblite.teavmlambda.demo;

import ca.weblite.teavmlambda.api.Platform;
import ca.weblite.teavmlambda.api.Router;
import ca.weblite.teavmlambda.api.db.Database;
import ca.weblite.teavmlambda.api.db.DatabaseFactory;
import ca.weblite.teavmlambda.generated.GeneratedRouter;

/**
 * Application entry point.
 * <p>
 * This class is platform-neutral — the same code runs on both Node.js (via TeaVM)
 * and on a standard JVM. The platform-specific adapter and database driver are
 * discovered automatically via ServiceLoader based on which dependencies are
 * on the classpath.
 */
public class Main {

    public static void main(String[] args) {
        String dbUrl = Platform.env("DATABASE_URL", "postgresql://demo:demo@localhost:5432/demo");

        Database db = DatabaseFactory.create(dbUrl);

        UsersResource usersResource = new UsersResource(db);
        HealthResource healthResource = new HealthResource();

        Router router = new GeneratedRouter(healthResource, usersResource);
        Platform.start(router);
    }
}
