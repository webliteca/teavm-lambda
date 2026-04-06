package ca.weblite.teavmlambda.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the security roles permitted to access method(s) in a resource class.
 * <p>
 * Can be applied at class level (applies to all methods) or method level (overrides class level).
 * The JWT token's {@code groups} claim is matched against the specified roles.
 * <p>
 * Requires a {@link ca.weblite.teavmlambda.api.auth.JwtValidator} to be registered in the
 * container for token validation.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.CLASS)
public @interface RolesAllowed {
    String[] value();
}
