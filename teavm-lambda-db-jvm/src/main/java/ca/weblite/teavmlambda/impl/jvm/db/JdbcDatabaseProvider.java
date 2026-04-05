package ca.weblite.teavmlambda.impl.jvm.db;

import ca.weblite.teavmlambda.api.db.Database;
import ca.weblite.teavmlambda.api.db.DatabaseProvider;

/**
 * JVM/JDBC implementation of {@link DatabaseProvider}.
 * Discovered via ServiceLoader.
 */
public class JdbcDatabaseProvider implements DatabaseProvider {

    @Override
    public Database create(String connectionUrl) {
        return new JdbcDatabase(connectionUrl);
    }
}
