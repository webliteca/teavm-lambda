package io.teavmlambda.db.api;

import java.util.ServiceLoader;

/**
 * Platform-neutral JSON parsing.
 * <p>
 * Discovers the appropriate implementation via {@link ServiceLoader}.
 * On Node.js/TeaVM, delegates to {@code JSON.parse}; on JVM, uses a
 * lightweight built-in parser.
 * <p>
 * Usage:
 * <pre>
 * DbRow row = Json.parse("{\"name\":\"Alice\",\"email\":\"alice@example.com\"}");
 * String name = row.getString("name");
 * </pre>
 */
public final class Json {

    private static volatile JsonProvider provider;

    private Json() {
    }

    /**
     * Explicitly sets the JSON provider.
     * Useful for testing or when ServiceLoader is not available (e.g. under TeaVM).
     */
    public static void setProvider(JsonProvider provider) {
        Json.provider = provider;
    }

    /**
     * Parses a JSON object string into a {@link DbRow}.
     *
     * @param json a JSON object string
     * @return a DbRow whose fields correspond to the JSON properties
     */
    public static DbRow parse(String json) {
        return getProvider().parse(json);
    }

    private static JsonProvider getProvider() {
        JsonProvider p = provider;
        if (p != null) {
            return p;
        }
        synchronized (Json.class) {
            p = provider;
            if (p != null) {
                return p;
            }
            try {
                ServiceLoader<JsonProvider> sl = ServiceLoader.load(JsonProvider.class);
                for (JsonProvider found : sl) {
                    Json.provider = found;
                    return found;
                }
            } catch (Exception ignored) {
            }
            throw new IllegalStateException(
                    "No JsonProvider found. Add teavm-lambda-db (for Node.js) or "
                    + "teavm-lambda-db-jvm (for JVM) to your dependencies.");
        }
    }
}
