package ca.weblite.teavmlambda.api;

/**
 * RFC 7807 Problem Details for HTTP APIs.
 * <p>
 * Provides a standardized format for error responses:
 * <pre>
 * {
 *   "type": "about:blank",
 *   "title": "Not Found",
 *   "status": 404,
 *   "detail": "User with id 42 was not found"
 * }
 * </pre>
 * <p>
 * Usage:
 * <pre>
 * return ProblemDetail.of(404, "User not found").toResponse();
 * </pre>
 */
public final class ProblemDetail {

    private final String type;
    private final String title;
    private final int status;
    private final String detail;
    private final String instance;

    private ProblemDetail(String type, String title, int status, String detail, String instance) {
        this.type = type;
        this.title = title;
        this.status = status;
        this.detail = detail;
        this.instance = instance;
    }

    /**
     * Creates a ProblemDetail with the given status and detail message.
     * Title is inferred from the status code.
     */
    public static ProblemDetail of(int status, String detail) {
        return new ProblemDetail("about:blank", titleForStatus(status), status, detail, null);
    }

    /**
     * Creates a 400 Bad Request problem detail.
     */
    public static ProblemDetail badRequest(String detail) {
        return of(400, detail);
    }

    /**
     * Creates a 404 Not Found problem detail.
     */
    public static ProblemDetail notFound(String detail) {
        return of(404, detail);
    }

    /**
     * Creates a 409 Conflict problem detail.
     */
    public static ProblemDetail conflict(String detail) {
        return of(409, detail);
    }

    /**
     * Creates a 500 Internal Server Error problem detail.
     */
    public static ProblemDetail internalError(String detail) {
        return of(500, detail);
    }

    /**
     * Returns a new ProblemDetail with the specified type URI.
     */
    public ProblemDetail type(String type) {
        return new ProblemDetail(type, this.title, this.status, this.detail, this.instance);
    }

    /**
     * Returns a new ProblemDetail with the specified instance URI.
     */
    public ProblemDetail instance(String instance) {
        return new ProblemDetail(this.type, this.title, this.status, this.detail, instance);
    }

    /**
     * Serializes to JSON.
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":").append(jsonString(type));
        sb.append(",\"title\":").append(jsonString(title));
        sb.append(",\"status\":").append(status);
        if (detail != null) {
            sb.append(",\"detail\":").append(jsonString(detail));
        }
        if (instance != null) {
            sb.append(",\"instance\":").append(jsonString(instance));
        }
        sb.append('}');
        return sb.toString();
    }

    /**
     * Converts to a {@link Response} with appropriate status and content type.
     */
    public Response toResponse() {
        return Response.status(status)
                .header("Content-Type", "application/problem+json")
                .body(toJson());
    }

    public String getType() { return type; }
    public String getTitle() { return title; }
    public int getStatus() { return status; }
    public String getDetail() { return detail; }
    public String getInstance() { return instance; }

    private static String jsonString(String value) {
        if (value == null) return "null";
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
    }

    private static String titleForStatus(int status) {
        switch (status) {
            case 400: return "Bad Request";
            case 401: return "Unauthorized";
            case 403: return "Forbidden";
            case 404: return "Not Found";
            case 405: return "Method Not Allowed";
            case 409: return "Conflict";
            case 422: return "Unprocessable Entity";
            case 429: return "Too Many Requests";
            case 500: return "Internal Server Error";
            case 502: return "Bad Gateway";
            case 503: return "Service Unavailable";
            default: return "Error";
        }
    }
}
