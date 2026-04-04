package io.teavmlambda.db.api;

/**
 * Platform-neutral representation of a database row.
 * On Node.js this wraps a JSObject; on JVM it wraps a JDBC ResultSet row.
 */
public interface DbRow {

    String getString(String column);

    int getInt(String column);

    double getDouble(String column);

    boolean getBoolean(String column);

    boolean has(String column);

    boolean isNull(String column);

    String toJson();
}
