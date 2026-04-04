package io.teavmlambda.logging;

import org.teavm.jso.JSBody;

public final class Logger {

    private final String name;

    public Logger(String name) {
        this.name = name;
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
        consoleLog(level, name, message, contextJson);
    }

    @JSBody(params = {"level", "loggerName", "message", "contextJson"}, script = ""
            + "var entry = {"
            + "  timestamp: new Date().toISOString(),"
            + "  level: level,"
            + "  logger: loggerName,"
            + "  message: message"
            + "};"
            + "if (contextJson) {"
            + "  try { var ctx = JSON.parse(contextJson); Object.assign(entry, ctx); }"
            + "  catch(e) { entry.context = contextJson; }"
            + "}"
            + "var line = JSON.stringify(entry);"
            + "if (level === 'ERROR') { console.error(line); }"
            + "else if (level === 'WARN') { console.warn(line); }"
            + "else if (level === 'DEBUG') { console.debug(line); }"
            + "else { console.log(line); }")
    private static native void consoleLog(String level, String loggerName, String message, String contextJson);

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    sb.append("\\\"");
                    break;
                case '\\':
                    sb.append("\\\\");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }
}
