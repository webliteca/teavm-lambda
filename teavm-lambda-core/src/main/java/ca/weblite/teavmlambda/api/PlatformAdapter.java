package ca.weblite.teavmlambda.api;

/**
 * SPI for platform-specific functionality.
 * <p>
 * Implementations are discovered via {@link java.util.ServiceLoader} or set
 * explicitly via {@link Platform#setAdapter(PlatformAdapter)}.
 */
public interface PlatformAdapter {

    /**
     * Reads an environment variable.
     *
     * @param name the variable name
     * @return the value, or empty string if not set
     */
    String env(String name);

    /**
     * Starts the platform-specific server/handler with the given router.
     */
    void start(Router router);
}
