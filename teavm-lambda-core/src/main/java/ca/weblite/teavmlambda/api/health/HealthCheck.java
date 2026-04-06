package ca.weblite.teavmlambda.api.health;

/**
 * A health check that reports the status of a system component.
 * <p>
 * Implementations should be lightweight and fast, as health checks
 * are called frequently by container orchestrators (Cloud Run, Kubernetes).
 */
@FunctionalInterface
public interface HealthCheck {

    /**
     * Performs the health check and returns the result.
     *
     * @return the health check result
     */
    HealthResult check();
}
