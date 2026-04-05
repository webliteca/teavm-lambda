package io.teavmlambda.demo;

import io.teavmlambda.core.Platform;
import io.teavmlambda.core.Router;
import io.teavmlambda.db.api.Database;
import io.teavmlambda.db.api.DatabaseFactory;
import io.teavmlambda.db.api.JsonUtil;
import io.teavmlambda.generated.GeneratedRouter;

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
        JsonUtil json = DatabaseFactory.jsonUtil();

        UsersResource usersResource = new UsersResource(db, json);
        HealthResource healthResource = new HealthResource();

        Router router = new GeneratedRouter(healthResource, usersResource);
        Platform.start(router);
    }
}
