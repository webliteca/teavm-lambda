package ca.weblite.teavmlambda.api;

import java.util.ServiceLoader;

/**
 * Platform abstraction that provides WORA (Write Once Run Anywhere) support.
 * <p>
 * Discovers the platform adapter via {@link ServiceLoader} and delegates
 * environment access and server startup to it. On Node.js/TeaVM the JS adapter
 * is used; on JVM the JVM adapter is used. User code never references
 * platform-specific classes directly.
 * <p>
 * Usage:
 * <pre>
 * String dbUrl = Platform.env("DATABASE_URL", "postgresql://localhost/demo");
 * Router router = new GeneratedRouter(...);
 * Platform.start(router);
 * </pre>
 */
public final class Platform {

    private static volatile PlatformAdapter adapter;

    private Platform() {
    }

    /**
     * Explicitly sets the platform adapter.
     * Useful for testing or when ServiceLoader is not available (e.g. under TeaVM).
     */
    public static void setAdapter(PlatformAdapter adapter) {
        Platform.adapter = adapter;
    }

    /**
     * Returns true if a PlatformAdapter has been set or discovered.
     */
    public static boolean isAvailable() {
        if (adapter != null) return true;
        try {
            getAdapter();
            return !(adapter instanceof DefaultJvmPlatformAdapter);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Reads an environment variable.
     *
     * @param name the variable name
     * @return the value, or empty string if not set
     */
    public static String env(String name) {
        return getAdapter().env(name);
    }

    /**
     * Reads an environment variable with a default fallback.
     *
     * @param name         the variable name
     * @param defaultValue value to return if the variable is not set or empty
     * @return the value or default
     */
    public static String env(String name, String defaultValue) {
        String value = getAdapter().env(name);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    /**
     * Starts the platform-specific server/handler with the given router.
     * <p>
     * On AWS Lambda (Node.js): exports the handler to module.exports.
     * On AWS Lambda (JVM): not applicable (handler is instantiated by Lambda runtime).
     * On Cloud Run (Node.js): starts Node.js HTTP server.
     * On Cloud Run (JVM): starts JDK HttpServer.
     */
    public static void start(Router router) {
        getAdapter().start(router);
    }

    private static PlatformAdapter getAdapter() {
        PlatformAdapter a = adapter;
        if (a != null) {
            return a;
        }
        synchronized (Platform.class) {
            a = adapter;
            if (a != null) {
                return a;
            }
            try {
                ServiceLoader<PlatformAdapter> sl = ServiceLoader.load(PlatformAdapter.class);
                for (PlatformAdapter found : sl) {
                    adapter = found;
                    return found;
                }
            } catch (Exception ignored) {
            }
            // Fallback: JVM default using System.getenv (start() will throw)
            a = new DefaultJvmPlatformAdapter();
            adapter = a;
            return a;
        }
    }

    private static final class DefaultJvmPlatformAdapter implements PlatformAdapter {
        @Override
        public String env(String name) {
            String val = System.getenv(name);
            return val != null ? val : "";
        }

        @Override
        public void start(Router router) {
            throw new UnsupportedOperationException(
                    "No PlatformAdapter found. Add teavm-lambda-adapter-*-jvm or "
                    + "teavm-lambda-adapter-* to your dependencies.");
        }
    }
}
