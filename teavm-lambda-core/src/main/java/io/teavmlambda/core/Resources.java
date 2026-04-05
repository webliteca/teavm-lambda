package io.teavmlambda.core;

import java.util.ServiceLoader;

/**
 * Loads classpath-style resources at runtime using a pluggable {@link ResourceLoader}.
 * <p>
 * The loader is discovered via {@link ServiceLoader}. If no implementation is found,
 * a default is used that attempts to read from a {@code resources/} directory relative
 * to the working directory (suitable for the Node.js/TeaVM deployment model).
 */
public final class Resources {

    private static volatile ResourceLoader loader;

    private Resources() {
    }

    /**
     * Explicitly sets the resource loader. Useful for testing or when ServiceLoader
     * is not available (e.g. under TeaVM).
     */
    public static void setLoader(ResourceLoader loader) {
        Resources.loader = loader;
    }

    /**
     * Returns true. Resources is always available — it falls back to
     * classpath loading on JVM if no explicit loader is registered.
     */
    public static boolean isAvailable() {
        return true;
    }

    /**
     * Loads a resource as text.
     *
     * @param name resource path, e.g. {@code "/openapi.json"} or {@code "openapi.json"}
     * @return the content as a string, or {@code null} if the resource does not exist
     */
    public static String loadText(String name) {
        if (name == null) {
            return null;
        }
        String path = name.startsWith("/") ? name.substring(1) : name;
        return getLoader().loadText(path);
    }

    private static ResourceLoader getLoader() {
        ResourceLoader l = loader;
        if (l != null) {
            return l;
        }
        synchronized (Resources.class) {
            l = loader;
            if (l != null) {
                return l;
            }
            try {
                ServiceLoader<ResourceLoader> sl = ServiceLoader.load(ResourceLoader.class);
                for (ResourceLoader found : sl) {
                    loader = found;
                    return found;
                }
            } catch (Exception ignored) {
                // ServiceLoader may not work under TeaVM; fall through to default
            }
            // Default: try classpath loading (works on JVM out of the box)
            l = new ClasspathResourceLoader();
            loader = l;
            return l;
        }
    }

    /**
     * Default resource loader that reads from the classpath.
     * Works on standard JVM. Under TeaVM, users should set a loader explicitly
     * or provide a ServiceLoader registration.
     */
    private static final class ClasspathResourceLoader implements ResourceLoader {
        @Override
        public String loadText(String name) {
            try {
                java.io.InputStream is = ClasspathResourceLoader.class.getClassLoader()
                        .getResourceAsStream(name);
                if (is == null) {
                    return null;
                }
                try (is) {
                    return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                }
            } catch (Exception e) {
                return null;
            }
        }
    }
}
