package ca.weblite.teavmlambda.api.json;

import java.util.ArrayList;
import java.util.List;

/**
 * Fluent JSON builder for constructing JSON objects and arrays without reflection.
 * <p>
 * Object usage:
 * <pre>
 * String json = JsonBuilder.object()
 *     .put("name", "Alice")
 *     .put("age", 30)
 *     .put("active", true)
 *     .build();
 * // {"name":"Alice","age":30,"active":true}
 * </pre>
 * <p>
 * Array usage:
 * <pre>
 * String json = JsonBuilder.array()
 *     .add(item1.toJson())
 *     .add(item2.toJson())
 *     .build();
 * // [{"name":"Alice"},{"name":"Bob"}]
 * </pre>
 */
public final class JsonBuilder {

    private final boolean isArray;
    private final List<String> entries = new ArrayList<>();

    private JsonBuilder(boolean isArray) {
        this.isArray = isArray;
    }

    /**
     * Creates a JSON object builder.
     */
    public static JsonBuilder object() {
        return new JsonBuilder(false);
    }

    /**
     * Creates a JSON array builder.
     */
    public static JsonBuilder array() {
        return new JsonBuilder(true);
    }

    /**
     * Adds a string key-value pair to the object.
     */
    public JsonBuilder put(String key, String value) {
        if (value == null) {
            entries.add("\"" + escapeJson(key) + "\":null");
        } else {
            entries.add("\"" + escapeJson(key) + "\":\"" + escapeJson(value) + "\"");
        }
        return this;
    }

    /**
     * Adds an integer key-value pair to the object.
     */
    public JsonBuilder put(String key, int value) {
        entries.add("\"" + escapeJson(key) + "\":" + value);
        return this;
    }

    /**
     * Adds a long key-value pair to the object.
     */
    public JsonBuilder put(String key, long value) {
        entries.add("\"" + escapeJson(key) + "\":" + value);
        return this;
    }

    /**
     * Adds a double key-value pair to the object.
     */
    public JsonBuilder put(String key, double value) {
        entries.add("\"" + escapeJson(key) + "\":" + value);
        return this;
    }

    /**
     * Adds a boolean key-value pair to the object.
     */
    public JsonBuilder put(String key, boolean value) {
        entries.add("\"" + escapeJson(key) + "\":" + value);
        return this;
    }

    /**
     * Adds a raw JSON value (object or array) with the given key.
     */
    public JsonBuilder putRaw(String key, String rawJson) {
        if (rawJson == null) {
            entries.add("\"" + escapeJson(key) + "\":null");
        } else {
            entries.add("\"" + escapeJson(key) + "\":" + rawJson);
        }
        return this;
    }

    /**
     * Adds a raw JSON element to the array.
     */
    public JsonBuilder add(String rawJson) {
        entries.add(rawJson);
        return this;
    }

    /**
     * Adds a string element to the array.
     */
    public JsonBuilder addString(String value) {
        if (value == null) {
            entries.add("null");
        } else {
            entries.add("\"" + escapeJson(value) + "\"");
        }
        return this;
    }

    /**
     * Builds the JSON string.
     */
    public String build() {
        StringBuilder sb = new StringBuilder();
        sb.append(isArray ? '[' : '{');
        for (int i = 0; i < entries.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(entries.get(i));
        }
        sb.append(isArray ? ']' : '}');
        return sb.toString();
    }

    private static String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
