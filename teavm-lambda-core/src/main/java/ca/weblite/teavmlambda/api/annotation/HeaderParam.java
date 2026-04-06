package ca.weblite.teavmlambda.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Binds a method parameter to an HTTP request header value.
 * <p>
 * Example:
 * <pre>
 * &#64;GET
 * public Response list(&#64;HeaderParam("X-Request-Id") String requestId) { ... }
 * </pre>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.CLASS)
public @interface HeaderParam {
    /**
     * The name of the HTTP header to bind. Case-insensitive at runtime.
     */
    String value();
}
