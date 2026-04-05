package ca.weblite.teavmlambda.api.logging;

import java.util.ServiceLoader;

public final class Logger {

    private final String name;
    private static volatile LogHandler handler;

    public Logger(String name) {
        this.name = name;
    }

    public static void setHandler(LogHandler handler) {
        Logger.handler = handler;
    }

    public static boolean isAvailable() {
        return true; // always available — falls back to stderr
    }

    public void debug(String message) {
        log("DEBUG", message, null);
    }

    public void debug(String message, String contextJson) {
        log("DEBUG", message, contextJson);
    }

    public void info(String message) {
        log("INFO", message, null);
    }

    public void info(String message, String contextJson) {
        log("INFO", message, contextJson);
    }

    public void warn(String message) {
        log("WARN", message, null);
    }

    public void warn(String message, String contextJson) {
        log("WARN", message, contextJson);
    }

    public void error(String message) {
        log("ERROR", message, null);
    }

    public void error(String message, String contextJson) {
        log("ERROR", message, contextJson);
    }

    public void error(String message, Throwable t) {
        String errorContext = "{\"error\":\"" + escapeJson(t.getClass().getName())
                + "\",\"errorMessage\":\"" + escapeJson(t.getMessage() != null ? t.getMessage() : "") + "\"}";
        log("ERROR", message, errorContext);
    }

    private void log(String level, String message, String contextJson) {
        getHandler().log(level, name, message, contextJson);
    }

    private static LogHandler getHandler() {
        LogHandler h = handler;
        if (h != null) return h;
        synchronized (Logger.class) {
            h = handler;
            if (h != null) return h;
            try {
                ServiceLoader<LogHandler> sl = ServiceLoader.load(LogHandler.class);
                for (LogHandler found : sl) {
                    handler = found;
                    return found;
                }
            } catch (Exception ignored) {}
            h = new StderrLogHandler();
            handler = h;
            return h;
        }
    }

    public static String escapeJson(String value) {
        if (value == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    private static final class StderrLogHandler implements LogHandler {
        @Override
        public void log(String level, String loggerName, String message, String contextJson) {
            StringBuilder entry = new StringBuilder();
            entry.append("{\"timestamp\":\"").append(java.time.Instant.now().toString()).append("\"");
            entry.append(",\"level\":\"").append(level).append("\"");
            entry.append(",\"logger\":\"").append(escapeJson(loggerName)).append("\"");
            entry.append(",\"message\":\"").append(escapeJson(message)).append("\"");
            if (contextJson != null && !contextJson.isEmpty()) {
                // Merge context fields into the entry
                String trimmed = contextJson.trim();
                if (trimmed.startsWith("{") && trimmed.endsWith("}") && trimmed.length() > 2) {
                    entry.append(",").append(trimmed.substring(1, trimmed.length() - 1));
                }
            }
            entry.append("}");
            System.err.println(entry);
        }
    }
}
