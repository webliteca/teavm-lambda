package ca.weblite.teavmlambda.impl.js;

import ca.weblite.teavmlambda.api.sentry.SentryHandler;
import org.teavm.jso.JSBody;

public class NodeSentryHandler implements SentryHandler {

    @Override
    public void init(String dsn, String environment) {
        sentryInit(dsn, environment);
    }

    @Override
    public void captureError(String name, String message) {
        sentryCaptureError(name, message);
    }

    @Override
    public void captureMessage(String message, String level) {
        sentryCaptureMessage(message, level);
    }

    @Override
    public void setTag(String key, String value) {
        sentrySetTag(key, value);
    }

    @Override
    public void setUser(String id, String email) {
        sentrySetUser(id, email);
    }

    @Override
    public void addBreadcrumb(String category, String message) {
        sentryAddBreadcrumb(category, message);
    }

    @Override
    public void configureRequestScope(String method, String path, String queryString) {
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

    public static void install() {
        ca.weblite.teavmlambda.api.sentry.Sentry.setHandler(new NodeSentryHandler());
    }
}
