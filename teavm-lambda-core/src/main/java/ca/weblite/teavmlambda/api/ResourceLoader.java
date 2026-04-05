package ca.weblite.teavmlambda.api;

/**
 * SPI for loading resources at runtime.
 * <p>
 * On Node.js (TeaVM), reads from the filesystem {@code resources/} directory.
 * On JVM, reads from the classpath via {@code ClassLoader.getResourceAsStream}.
 * <p>
 * Implementations are discovered via {@link java.util.ServiceLoader}.
 */
public interface ResourceLoader {

    /**
     * Loads a resource as text.
     *
     * @param name resource path, e.g. {@code "openapi.json"}
     * @return the content as a string, or {@code null} if the resource does not exist
     */
    String loadText(String name);
}
