package ca.weblite.teavmlambda.api.validation;

import ca.weblite.teavmlambda.api.Response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds a collection of {@link ValidationError}s and provides convenience methods
 * for checking validity and converting to a response.
 */
public final class ValidationResult {

    private final List<ValidationError> errors;

    public ValidationResult() {
        this.errors = new ArrayList<>();
    }

    public ValidationResult addError(String field, String message) {
        errors.add(new ValidationError(field, message));
        return this;
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    public List<ValidationError> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    /**
     * Serializes validation errors to JSON.
     * <pre>
     * {"errors":[{"field":"name","message":"must not be null"}]}
     * </pre>
     */
    public String toJson() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"errors\":[");
        for (int i = 0; i < errors.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(errors.get(i).toJson());
        }
        sb.append("]}");
        return sb.toString();
    }

    /**
     * Returns a 400 Bad Request response with validation errors.
     */
    public Response toResponse() {
        return Response.status(400)
                .header("Content-Type", "application/json")
                .body(toJson());
    }
}
