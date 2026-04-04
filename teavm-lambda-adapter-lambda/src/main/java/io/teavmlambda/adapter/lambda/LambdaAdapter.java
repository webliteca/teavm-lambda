package io.teavmlambda.adapter.lambda;

import io.teavmlambda.core.Request;
import io.teavmlambda.core.Response;
import io.teavmlambda.core.Router;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

import java.util.HashMap;
import java.util.Map;

public final class LambdaAdapter {

    private static Router router;

    private LambdaAdapter() {
    }

    public static void start(Router router) {
        LambdaAdapter.router = router;
        registerHandler(LambdaAdapter::handleRequest);
    }

    @JSFunctor
    interface HandlerFunction extends JSObject {
        JSObject handle(JSObject event);
    }

    @JSBody(params = {"fn"}, script = ""
            + "if (typeof module !== 'undefined') {"
            + "  module.exports.handler = function(event, context) {"
            + "    return Promise.resolve(fn(event));"
            + "  };"
            + "}"
            + "if (typeof globalThis !== 'undefined') {"
            + "  globalThis.__teavmLambdaHandler = fn;"
            + "}")
    private static native void registerHandler(HandlerFunction fn);

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

    static JSObject handleRequest(JSObject event) {
        String httpMethod = getEventMethod(event);
        String path = getEventPath(event);
        String body = getEventBody(event);

        Map<String, String> headers = entriesToMap(jsObjectToEntries(getEventHeaders(event)));
        Map<String, String> queryParams = entriesToMap(jsObjectToEntries(getEventQueryParams(event)));

        Request request = new Request(httpMethod, path, headers, queryParams, body);
        Response response = router.route(request);

        String[] headersArr = mapToEntries(response.getHeaders());
        return createApiGatewayResponse(response.getStatusCode(), response.getBody(), headersArr);
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
