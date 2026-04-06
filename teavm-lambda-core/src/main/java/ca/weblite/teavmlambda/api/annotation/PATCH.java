package ca.weblite.teavmlambda.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as handling HTTP PATCH requests.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.CLASS)
public @interface PATCH {
}
