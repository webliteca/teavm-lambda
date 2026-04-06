package ca.weblite.teavmlambda.api.json;

import ca.weblite.teavmlambda.api.db.DbRow;
import ca.weblite.teavmlambda.api.db.Json;

/**
 * Convenience wrapper around the platform-neutral {@link Json#parse(String)} API
 * that provides typed accessors for reading JSON objects.
 * <p>
 * Usage:
 * <pre>
 * JsonReader reader = JsonReader.parse(body);
 * String name = reader.getString("name");
 * int age = reader.getInt("age", 0);
 * boolean active = reader.getBoolean("active", false);
 * </pre>
 */
public final class JsonReader {

    private final DbRow row;

    private JsonReader(DbRow row) {
        this.row = row;
    }

    /**
     * Parses a JSON string into a JsonReader.
     *
     * @param json the JSON string to parse
     * @return a JsonReader wrapping the parsed data
     */
    public static JsonReader parse(String json) {
        return new JsonReader(Json.parse(json));
    }

    /**
     * Gets a string value, or null if the key is missing.
     */
    public String getString(String key) {
        if (!row.has(key) || row.isNull(key)) return null;
        return row.getString(key);
    }

    /**
     * Gets a string value with a default.
     */
    public String getString(String key, String defaultValue) {
        String value = getString(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Gets an integer value, or the default if the key is missing.
     */
    public int getInt(String key, int defaultValue) {
        if (!row.has(key) || row.isNull(key)) return defaultValue;
        return row.getInt(key);
    }

    /**
     * Gets a double value, or the default if the key is missing.
     */
    public double getDouble(String key, double defaultValue) {
        if (!row.has(key) || row.isNull(key)) return defaultValue;
        return row.getDouble(key);
    }

    /**
     * Gets a boolean value, or the default if the key is missing.
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        if (!row.has(key) || row.isNull(key)) return defaultValue;
        return row.getBoolean(key);
    }

    /**
     * Returns true if the key exists and is not null.
     */
    public boolean has(String key) {
        return row.has(key) && !row.isNull(key);
    }

    /**
     * Returns the underlying DbRow for advanced access.
     */
    public DbRow getRow() {
        return row;
    }
}
