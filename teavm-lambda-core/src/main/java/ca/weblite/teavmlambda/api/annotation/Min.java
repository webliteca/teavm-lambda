package ca.weblite.teavmlambda.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that the annotated numeric parameter is at least the specified value.
 * The string parameter is parsed as a long for comparison.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.CLASS)
public @interface Min {
    long value();
    String message() default "";
}
