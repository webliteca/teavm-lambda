package io.teavmlambda.db.api;

import java.util.ServiceLoader;

/**
 * Factory for creating {@link Database} instances in a platform-neutral way.
 * <p>
 * Discovers the appropriate implementation via {@link ServiceLoader}.
 * On Node.js/TeaVM, this creates a JSO-backed database; on JVM, a JDBC-backed one.
 * <p>
 * Usage:
 * <pre>
 * Database db = DatabaseFactory.create("postgresql://user:pass@host:port/db");
 * </pre>
 */
public final class DatabaseFactory {

    private static volatile DatabaseProvider provider;

    private DatabaseFactory() {
    }

    /**
     * Explicitly sets the database provider.
     * Useful for testing or when ServiceLoader is not available (e.g. under TeaVM).
     */
    public static void setProvider(DatabaseProvider provider) {
        DatabaseFactory.provider = provider;
    }

    /**
     * Returns true if a DatabaseProvider has been set or discovered via ServiceLoader.
     */
    public static boolean isAvailable() {
        if (provider != null) return true;
        try {
            getProvider();
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    /**
     * Creates a new Database instance connected to the given URL.
     *
     * @param connectionUrl a PostgreSQL-style connection URL
     *                      (e.g. {@code postgresql://user:pass@host:port/db})
     * @return a Database instance
     * @throws IllegalStateException if no provider is available
     */
    public static Database create(String connectionUrl) {
        return getProvider().create(connectionUrl);
    }

    private static DatabaseProvider getProvider() {
        DatabaseProvider p = provider;
        if (p != null) {
            return p;
        }
        synchronized (DatabaseFactory.class) {
            p = provider;
            if (p != null) {
                return p;
            }
            try {
                ServiceLoader<DatabaseProvider> sl = ServiceLoader.load(DatabaseProvider.class);
                for (DatabaseProvider found : sl) {
                    provider = found;
                    return found;
                }
            } catch (Exception ignored) {
            }
            throw new IllegalStateException(
                    "No DatabaseProvider found. Add teavm-lambda-db (for Node.js) or "
                    + "teavm-lambda-db-jvm (for JVM) to your dependencies.");
        }
    }
}
