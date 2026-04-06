package ca.weblite.teavmlambda.api.health;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The result of a {@link HealthCheck}.
 */
public final class HealthResult {

    public enum Status { UP, DOWN }

    private final Status status;
    private final Map<String, String> details;

    private HealthResult(Status status, Map<String, String> details) {
        this.status = status;
        this.details = Collections.unmodifiableMap(new LinkedHashMap<>(details));
    }

    /**
     * Creates a healthy result.
     */
    public static HealthResult up() {
        return new HealthResult(Status.UP, Collections.emptyMap());
    }

    /**
     * Creates a healthy result with details.
     */
    public static HealthResult up(Map<String, String> details) {
        return new HealthResult(Status.UP, details);
    }

    /**
     * Creates an unhealthy result with an error message.
     */
    public static HealthResult down(String error) {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("error", error);
        return new HealthResult(Status.DOWN, details);
    }

    /**
     * Creates an unhealthy result from an exception.
     */
    public static HealthResult down(Throwable t) {
        return down(t.getClass().getName() + ": " + t.getMessage());
    }

    public Status getStatus() { return status; }
    public Map<String, String> getDetails() { return details; }
    public boolean isUp() { return status == Status.UP; }

    /**
     * Serializes this result as a JSON object.
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"status\":\"").append(status.name()).append('"');
        if (!details.isEmpty()) {
            sb.append(",\"details\":{");
            boolean first = true;
            for (Map.Entry<String, String> entry : details.entrySet()) {
                if (!first) sb.append(',');
                sb.append('"').append(escapeJson(entry.getKey())).append("\":\"")
                  .append(escapeJson(entry.getValue())).append('"');
                first = false;
            }
            sb.append('}');
        }
        sb.append('}');
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
