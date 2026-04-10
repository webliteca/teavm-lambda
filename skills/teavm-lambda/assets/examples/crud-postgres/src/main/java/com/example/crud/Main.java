package com.example.crud;

import ca.weblite.teavmlambda.api.Container;
import ca.weblite.teavmlambda.api.Platform;
import ca.weblite.teavmlambda.api.Router;
import ca.weblite.teavmlambda.api.db.Database;
import ca.weblite.teavmlambda.api.db.DatabaseFactory;
import ca.weblite.teavmlambda.generated.GeneratedContainer;
import ca.weblite.teavmlambda.generated.GeneratedRouter;

public class Main {
    public static void main(String[] args) {
        String dbUrl = Platform.env("DATABASE_URL",
                "postgresql://demo:demo@localhost:5432/demo");

        Container container = new GeneratedContainer();
        container.register(Database.class, DatabaseFactory.create(dbUrl));

        Router router = new GeneratedRouter(container);
        Platform.start(router);
    }
}
