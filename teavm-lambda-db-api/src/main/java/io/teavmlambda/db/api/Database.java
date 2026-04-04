package io.teavmlambda.db.api;

/**
 * Platform-neutral database interface.
 * <p>
 * On Node.js, backed by the pg driver via JSO interop.
 * On JVM, backed by JDBC.
 */
public interface Database {

    DbResult query(String sql);

    DbResult query(String sql, String... params);

    void close();
}
