package ca.weblite.teavmlambda.api.validation;

/**
 * A single validation error for a request parameter.
 */
public final class ValidationError {

    private final String field;
    private final String message;

    public ValidationError(String field, String message) {
        this.field = field;
        this.message = message;
    }

    public String getField() { return field; }
    public String getMessage() { return message; }

    public String toJson() {
        return "{\"field\":\"" + escapeJson(field) + "\",\"message\":\"" + escapeJson(message) + "\"}";
    }

    private static String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
    }
}
