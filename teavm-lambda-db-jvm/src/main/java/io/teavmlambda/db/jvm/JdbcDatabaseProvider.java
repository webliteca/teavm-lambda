package io.teavmlambda.db.jvm;

import io.teavmlambda.db.api.Database;
import io.teavmlambda.db.api.DatabaseProvider;

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
