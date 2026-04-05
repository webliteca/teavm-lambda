package ca.weblite.teavmlambda.api.sentry;

/**
 * SPI for platform-specific Sentry integration.
 * On Node.js, uses @sentry/node via JSO. On JVM, could use io.sentry:sentry-java.
 */
public interface SentryHandler {
    void init(String dsn, String environment);
    void captureError(String name, String message);
    void captureMessage(String message, String level);
    void setTag(String key, String value);
    void setUser(String id, String email);
    void addBreadcrumb(String category, String message);
    void configureRequestScope(String method, String path, String queryString);
}
