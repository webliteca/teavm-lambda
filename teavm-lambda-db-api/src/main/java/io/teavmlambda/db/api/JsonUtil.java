package io.teavmlambda.db.api;

/**
 * Platform-neutral JSON utility interface.
 * <p>
 * On Node.js, delegates to JSON.parse/JSON.stringify.
 * On JVM, uses a lightweight manual implementation.
 */
public interface JsonUtil {

    DbRow parseJson(String json);

    String toJson(DbRow row);
}
