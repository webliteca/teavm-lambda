package io.teavmlambda.sentry;

import org.teavm.jso.JSBody;

public final class Sentry {

    private static boolean initialized;

    private Sentry() {
    }

    public static void init(String dsn) {
        init(dsn, null);
    }

    public static void init(String dsn, String environment) {
        if (dsn == null || dsn.isEmpty()) {
            return;
        }
        String env = (environment != null && !environment.isEmpty()) ? environment : null;
        sentryInit(dsn, env);
        initialized = true;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static void captureException(Throwable t) {
        if (!initialized) {
            return;
        }
        String name = t.getClass().getName();
        String message = t.getMessage() != null ? t.getMessage() : name;
        sentryCaptureError(name, message);
    }

    public static void captureMessage(String message) {
        captureMessage(message, "info");
    }

    public static void captureMessage(String message, String level) {
        if (!initialized) {
            return;
        }
        sentryCaptureMessage(message, level);
    }

    public static void setTag(String key, String value) {
        if (!initialized) {
            return;
        }
        sentrySetTag(key, value);
    }

    public static void setUser(String id, String email) {
        if (!initialized) {
            return;
        }
        sentrySetUser(id, email);
    }

    public static void addBreadcrumb(String category, String message) {
        if (!initialized) {
            return;
        }
        sentryAddBreadcrumb(category, message);
    }

    public static void configureRequestScope(String method, String path, String queryString) {
        if (!initialized) {
            return;
        }
        sentryConfigureRequestScope(method, path, queryString);
    }

    @JSBody(params = {"dsn", "environment"}, script = ""
            + "var Sentry = require('@sentry/node');"
            + "var opts = { dsn: dsn };"
            + "if (environment) { opts.environment = environment; }"
            + "Sentry.init(opts);"
            + "globalThis.__sentry = Sentry;")
    private static native void sentryInit(String dsn, String environment);

    @JSBody(params = {"name", "message"}, script = ""
            + "var Sentry = globalThis.__sentry;"
            + "if (!Sentry) return;"
            + "var err = new Error(message);"
            + "err.name = name;"
            + "Sentry.captureException(err);")
    private static native void sentryCaptureError(String name, String message);

    @JSBody(params = {"message", "level"}, script = ""
            + "var Sentry = globalThis.__sentry;"
            + "if (!Sentry) return;"
            + "Sentry.captureMessage(message, level);")
    private static native void sentryCaptureMessage(String message, String level);

    @JSBody(params = {"key", "value"}, script = ""
            + "var Sentry = globalThis.__sentry;"
            + "if (!Sentry) return;"
            + "Sentry.setTag(key, value);")
    private static native void sentrySetTag(String key, String value);

    @JSBody(params = {"id", "email"}, script = ""
            + "var Sentry = globalThis.__sentry;"
            + "if (!Sentry) return;"
            + "Sentry.setUser({ id: id, email: email || undefined });")
    private static native void sentrySetUser(String id, String email);

    @JSBody(params = {"category", "message"}, script = ""
            + "var Sentry = globalThis.__sentry;"
            + "if (!Sentry) return;"
            + "Sentry.addBreadcrumb({ category: category, message: message, level: 'info' });")
    private static native void sentryAddBreadcrumb(String category, String message);

    @JSBody(params = {"method", "path", "queryString"}, script = ""
            + "var Sentry = globalThis.__sentry;"
            + "if (!Sentry) return;"
            + "Sentry.withScope(function(scope) {"
            + "  scope.setTransactionName(method + ' ' + path);"
            + "  scope.setTag('http.method', method);"
            + "  scope.setTag('http.path', path);"
            + "  if (queryString) { scope.setTag('http.query', queryString); }"
            + "});")
    private static native void sentryConfigureRequestScope(String method, String path, String queryString);
}
