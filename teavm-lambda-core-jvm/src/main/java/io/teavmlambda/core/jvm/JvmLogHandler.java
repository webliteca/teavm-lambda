package io.teavmlambda.core.jvm;

import io.teavmlambda.logging.LogHandler;
import io.teavmlambda.logging.Logger;

import java.time.Instant;

/**
 * JVM LogHandler that writes structured JSON to stderr.
 * Same format as the Node.js handler for consistent log processing.
 */
public class JvmLogHandler implements LogHandler {

    @Override
    public void log(String level, String loggerName, String message, String contextJson) {
        StringBuilder entry = new StringBuilder();
        entry.append("{\"timestamp\":\"").append(Instant.now().toString()).append("\"");
        entry.append(",\"level\":\"").append(level).append("\"");
        entry.append(",\"logger\":\"").append(Logger.escapeJson(loggerName)).append("\"");
        entry.append(",\"message\":\"").append(Logger.escapeJson(message)).append("\"");
        if (contextJson != null && !contextJson.isEmpty()) {
            String trimmed = contextJson.trim();
            if (trimmed.startsWith("{") && trimmed.endsWith("}") && trimmed.length() > 2) {
                entry.append(",").append(trimmed.substring(1, trimmed.length() - 1));
            }
        }
        entry.append("}");
        System.err.println(entry);
    }
}
