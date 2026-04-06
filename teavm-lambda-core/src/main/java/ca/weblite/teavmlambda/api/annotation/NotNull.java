package ca.weblite.teavmlambda.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that the annotated parameter is not null.
 * <p>
 * The annotation processor generates validation code that returns
 * 400 Bad Request if the parameter is null.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.CLASS)
public @interface NotNull {
    String message() default "must not be null";
}
