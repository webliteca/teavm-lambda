package io.teavmlambda.core.js;

import io.teavmlambda.logging.LogHandler;
import org.teavm.jso.JSBody;

public class NodeLogHandler implements LogHandler {

    @Override
    public void log(String level, String loggerName, String message, String contextJson) {
        consoleLog(level, loggerName, message, contextJson);
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

    public static void install() {
        io.teavmlambda.logging.Logger.setHandler(new NodeLogHandler());
    }
}
