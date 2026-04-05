package ca.weblite.teavmlambda.impl.js.db;

import ca.weblite.teavmlambda.api.db.Database;
import ca.weblite.teavmlambda.api.db.DatabaseProvider;

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
        ca.weblite.teavmlambda.api.db.DatabaseFactory.setProvider(new JsDatabaseProvider());
    }
}
