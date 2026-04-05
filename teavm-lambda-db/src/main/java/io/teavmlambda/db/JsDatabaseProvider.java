package io.teavmlambda.db;

import io.teavmlambda.db.api.Database;
import io.teavmlambda.db.api.DatabaseProvider;

/**
 * Node.js/TeaVM implementation of {@link DatabaseProvider}.
 * Creates JSO-backed Database instances using the Node.js pg driver.
 */
public class JsDatabaseProvider implements DatabaseProvider {

    @Override
    public Database create(String connectionUrl) {
        return new JsDatabase(connectionUrl);
    }

    /**
     * Registers this provider with the DatabaseFactory.
     * Call this early in your application startup (e.g. in Main or from a PlatformAdapter).
     */
    public static void install() {
        io.teavmlambda.db.api.DatabaseFactory.setProvider(new JsDatabaseProvider());
    }
}
