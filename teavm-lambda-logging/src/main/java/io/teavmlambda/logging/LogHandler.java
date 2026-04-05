package io.teavmlambda.logging;

/**
 * SPI for platform-specific log output.
 * On Node.js, writes structured JSON to console.log/error/warn/debug.
 * On JVM, writes structured JSON to stderr (default) or java.util.logging.
 */
public interface LogHandler {
    void log(String level, String loggerName, String message, String contextJson);
}
