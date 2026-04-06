package ca.weblite.teavmlambda.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies that all security roles are permitted to access the annotated method or class.
 * <p>
 * When applied to a method, it overrides any class-level {@link RolesAllowed} annotation.
 * Useful for public endpoints within an otherwise secured resource.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.CLASS)
public @interface PermitAll {
}
