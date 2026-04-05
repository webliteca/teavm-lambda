package ca.weblite.teavmlambda.impl.jvm;

import ca.weblite.teavmlambda.api.sentry.SentryHandler;

import java.util.logging.Logger;

/**
 * No-op Sentry handler for JVM.
 * Logs a warning on init that Sentry is not configured, then silently discards events.
 * <p>
 * Replace with a real implementation (backed by io.sentry:sentry-java) for production use.
 */
public class NoOpSentryHandler implements SentryHandler {

    private static final Logger logger = Logger.getLogger(NoOpSentryHandler.class.getName());

    @Override
    public void init(String dsn, String environment) {
        logger.warning("Sentry DSN provided but no JVM Sentry SDK configured. "
                + "Add a SentryHandler implementation backed by io.sentry:sentry-java for production use.");
    }

    @Override
    public void captureError(String name, String message) { }

    @Override
    public void captureMessage(String message, String level) { }

    @Override
    public void setTag(String key, String value) { }

    @Override
    public void setUser(String id, String email) { }

    @Override
    public void addBreadcrumb(String category, String message) { }

    @Override
    public void configureRequestScope(String method, String path, String queryString) { }
}
