package io.teavmlambda.sentry;

import java.util.ServiceLoader;

public final class Sentry {

    private static volatile SentryHandler handler;
    private static boolean initialized;

    private Sentry() {
    }

    public static void setHandler(SentryHandler handler) {
        Sentry.handler = handler;
    }

    /**
     * Returns true if a SentryHandler implementation is available on the classpath.
     */
    public static boolean isAvailable() {
        if (handler != null) return true;
        try {
            ServiceLoader<SentryHandler> sl = ServiceLoader.load(SentryHandler.class);
            for (SentryHandler found : sl) {
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    public static void init(String dsn) {
        init(dsn, null);
    }

    public static void init(String dsn, String environment) {
        if (dsn == null || dsn.isEmpty()) {
            return;
        }
        SentryHandler h = getHandler();
        if (h == null) return;
        String env = (environment != null && !environment.isEmpty()) ? environment : null;
        h.init(dsn, env);
        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static void captureException(Throwable t) {
        if (!initialized) return;
        String name = t.getClass().getName();
        String message = t.getMessage() != null ? t.getMessage() : name;
        getHandler().captureError(name, message);
    }

    public static void captureMessage(String message) {
        captureMessage(message, "info");
    }

    public static void captureMessage(String message, String level) {
        if (!initialized) return;
        getHandler().captureMessage(message, level);
    }

    public static void setTag(String key, String value) {
        if (!initialized) return;
        getHandler().setTag(key, value);
    }

    public static void setUser(String id, String email) {
        if (!initialized) return;
        getHandler().setUser(id, email);
    }

    public static void addBreadcrumb(String category, String message) {
        if (!initialized) return;
        getHandler().addBreadcrumb(category, message);
    }

    public static void configureRequestScope(String method, String path, String queryString) {
        if (!initialized) return;
        getHandler().configureRequestScope(method, path, queryString);
    }

    private static SentryHandler getHandler() {
        SentryHandler h = handler;
        if (h != null) return h;
        synchronized (Sentry.class) {
            h = handler;
            if (h != null) return h;
            try {
                ServiceLoader<SentryHandler> sl = ServiceLoader.load(SentryHandler.class);
                for (SentryHandler found : sl) {
                    handler = found;
                    return found;
                }
            } catch (Exception ignored) {}
            return null;
        }
    }
}
