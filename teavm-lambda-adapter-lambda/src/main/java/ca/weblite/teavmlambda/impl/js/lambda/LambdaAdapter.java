package ca.weblite.teavmlambda.impl.js.lambda;

import ca.weblite.teavmlambda.api.Request;
import ca.weblite.teavmlambda.api.Response;
import ca.weblite.teavmlambda.api.Router;
import ca.weblite.teavmlambda.impl.js.NodeLogHandler;
import ca.weblite.teavmlambda.impl.js.NodeResourceLoader;
import ca.weblite.teavmlambda.api.logging.Logger;
import ca.weblite.teavmlambda.api.sentry.Sentry;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

import java.util.HashMap;
import java.util.Map;

public final class LambdaAdapter {

    private static Router router;
    private static final Logger logger = new Logger("LambdaAdapter");

    private LambdaAdapter() {
    }

    public static void start(Router router) {
        LambdaAdapter.router = router;
        NodeResourceLoader.install();
        NodeLogHandler.install();
        initMonitoring();
        exportHandler(LambdaAdapter::handleRequest);
        logger.info("Lambda handler registered");
    }

    private static void initMonitoring() {
        String sentryDsn = getEnv("SENTRY_DSN");
        if (sentryDsn != null && !sentryDsn.isEmpty()) {
            String environment = getEnv("SENTRY_ENVIRONMENT");
            Sentry.init(sentryDsn, environment);
            Sentry.setTag("platform", "lambda");
            logger.info("Sentry initialized");
        }
    }

    @JSBody(params = {"name"}, script = "return process.env[name] || '';")
    private static native String getEnv(String name);

    @JSFunctor
    interface RequestHandler extends JSObject {
        void handle(JSObject event, ResultCallback resolve, ResultCallback reject);
    }

    @JSFunctor
    interface ResultCallback extends JSObject {
        void call(JSObject result);
    }

    @JSBody(params = {"javaHandler"}, script = ""
            + "function teavmHandler(event, context) {"
            + "  return new Promise(function(resolve, reject) {"
            + "    $rt_startThread(function() {"
            + "      javaHandler(event,"
            + "        function(result) { resolve(result); },"
            + "        function(err) { reject(err); }"
            + "      );"
            + "    });"
            + "  });"
            + "}"
            + "if (typeof module !== 'undefined') {"
            + "  module.exports.handler = teavmHandler;"
            + "}"
            + "if (typeof globalThis !== 'undefined') {"
            + "  globalThis.__teavmLambdaHandler = teavmHandler;"
            + "}")
    private static native void exportHandler(RequestHandler javaHandler);

    @JSBody(params = {"event"}, script =
            "return event.httpMethod || (event.requestContext && event.requestContext.http && event.requestContext.http.method) || 'GET';")
    private static native String getEventMethod(JSObject event);

    @JSBody(params = {"event"}, script = "return event.path || event.rawPath || '/';")
    private static native String getEventPath(JSObject event);

    @JSBody(params = {"event"}, script = "return event.body || null;")
    private static native String getEventBody(JSObject event);

    @JSBody(params = {"event"}, script = "return event.headers || {};")
    private static native JSObject getEventHeaders(JSObject event);

    @JSBody(params = {"event"}, script = "return event.queryStringParameters || {};")
    private static native JSObject getEventQueryParams(JSObject event);

    @JSBody(params = {"obj"}, script = ""
            + "var keys = Object.keys(obj || {});"
            + "var result = [];"
            + "for (var i = 0; i < keys.length; i++) {"
            + "  result.push(keys[i]);"
            + "  result.push(String(obj[keys[i]]));"
            + "}"
            + "return result;")
    private static native String[] jsObjectToEntries(JSObject obj);

    @JSBody(params = {"statusCode", "body", "headersArr"}, script = ""
            + "var headers = {};"
            + "for (var i = 0; i < headersArr.length; i += 2) {"
            + "  headers[headersArr[i]] = headersArr[i + 1];"
            + "}"
            + "return { statusCode: statusCode, headers: headers, body: body || '' };")
    private static native JSObject createApiGatewayResponse(int statusCode, String body, String[] headersArr);

    @JSBody(params = {"message"}, script = "return { message: String(message) };")
    private static native JSObject createErrorObject(String message);

    @JSBody(script = "return typeof process !== 'undefined' && typeof process.hrtime === 'function'"
            + " ? Number(process.hrtime.bigint()) / 1e6 : Date.now();")
    private static native double now();

    static void handleRequest(JSObject event, ResultCallback resolve, ResultCallback reject) {
        double startTime = now();
        String httpMethod = null;
        String path = null;
        try {
            httpMethod = getEventMethod(event);
            path = getEventPath(event);
            String body = getEventBody(event);

            Map<String, String> headers = entriesToMap(jsObjectToEntries(getEventHeaders(event)));
            Map<String, String> queryParams = entriesToMap(jsObjectToEntries(getEventQueryParams(event)));

            Sentry.addBreadcrumb("http", httpMethod + " " + path);

            Request request = new Request(httpMethod, path, headers, queryParams, body);
            Response response = router.route(request);

            double duration = now() - startTime;
            logger.info("request completed",
                    "{\"method\":\"" + httpMethod
                    + "\",\"path\":\"" + path
                    + "\",\"status\":" + response.getStatusCode()
                    + ",\"duration_ms\":" + Math.round(duration) + "}");

            String[] headersArr = mapToEntries(response.getHeaders());
            JSObject result = createApiGatewayResponse(response.getStatusCode(), response.getBody(), headersArr);
            resolve.call(result);
        } catch (Exception e) {
            double duration = now() - startTime;
            logger.error("request failed", e);
            logger.error("request error context",
                    "{\"method\":\"" + (httpMethod != null ? httpMethod : "unknown")
                    + "\",\"path\":\"" + (path != null ? path : "unknown")
                    + "\",\"duration_ms\":" + Math.round(duration) + "}");
            Sentry.captureException(e);
            reject.call(createErrorObject(e.getMessage()));
        }
    }

    private static Map<String, String> entriesToMap(String[] entries) {
        Map<String, String> map = new HashMap<>();
        if (entries != null) {
            for (int i = 0; i + 1 < entries.length; i += 2) {
                map.put(entries[i], entries[i + 1]);
            }
        }
        return map;
    }

    private static String[] mapToEntries(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return new String[0];
        }
        String[] entries = new String[map.size() * 2];
        int i = 0;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            entries[i++] = entry.getKey();
            entries[i++] = entry.getValue();
        }
        return entries;
    }
}
