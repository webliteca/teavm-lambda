package io.teavmlambda.db.api;

/**
 * SPI for creating platform-specific {@link Database} and {@link JsonUtil} instances.
 * <p>
 * Implementations are discovered via {@link java.util.ServiceLoader} or set
 * explicitly via {@link DatabaseFactory#setProvider(DatabaseProvider)}.
 */
public interface DatabaseProvider {

    /**
     * Creates a new Database instance connected to the given URL.
     *
     * @param connectionUrl a PostgreSQL-style connection URL
     * @return a Database instance
     */
    Database create(String connectionUrl);

    /**
     * Creates a new JsonUtil instance for the current platform.
     *
     * @return a JsonUtil instance
     */
    JsonUtil jsonUtil();
}
