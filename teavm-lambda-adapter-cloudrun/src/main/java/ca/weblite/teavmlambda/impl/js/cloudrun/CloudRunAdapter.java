package ca.weblite.teavmlambda.impl.js.cloudrun;

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

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public final class CloudRunAdapter {

    private static Router router;
    private static final Logger logger = new Logger("CloudRunAdapter");

    private CloudRunAdapter() {
    }

    public static void start(Router router) {
        CloudRunAdapter.router = router;
        NodeResourceLoader.install();
        NodeLogHandler.install();
        initMonitoring();
        String port = getEnv("PORT");
        if (port == null || port.isEmpty()) {
            port = "8080";
        }
        startServer(CloudRunAdapter::handleRequest, Integer.parseInt(port));
    }

    private static void initMonitoring() {
        String sentryDsn = getEnv("SENTRY_DSN");
        if (sentryDsn != null && !sentryDsn.isEmpty()) {
            String environment = getEnv("SENTRY_ENVIRONMENT");
            Sentry.init(sentryDsn, environment);
            Sentry.setTag("platform", "cloudrun");
            logger.info("Sentry initialized");
        }
    }

    @JSFunctor
    interface RequestCallback extends JSObject {
        void handle(JSObject req, JSObject res);
    }

    @JSBody(params = {"callback", "port"}, script = ""
            + "var http = require('http');"
            + "var url = require('url');"
            + "var fs = require('fs');"
            + "var pathMod = require('path');"
            + "var publicDir = pathMod.join(process.cwd(), 'public');"
            + "var hasPublicDir = fs.existsSync(publicDir);"
            + "var mimeTypes = {"
            + "  '.html':'text/html','.css':'text/css','.js':'application/javascript',"
            + "  '.json':'application/json','.png':'image/png','.jpg':'image/jpeg',"
            + "  '.jpeg':'image/jpeg','.gif':'image/gif','.svg':'image/svg+xml',"
            + "  '.ico':'image/x-icon','.woff':'font/woff','.woff2':'font/woff2',"
            + "  '.ttf':'font/ttf','.otf':'font/otf','.txt':'text/plain',"
            + "  '.xml':'application/xml','.webp':'image/webp','.map':'application/json'"
            + "};"
            + "var inFlight = 0;"
            + "var shuttingDown = false;"
            + "var server = http.createServer(function(req, res) {"
            + "  if (shuttingDown) { res.writeHead(503); res.end('Service Unavailable'); return; }"
            + "  var parsed = url.parse(req.url, true);"
            + "  var pathname = parsed.pathname || '/';"
            + "  if (hasPublicDir && req.method === 'GET') {"
            + "    var safePath = pathMod.normalize(pathname).replace(/^\\.\\.([\\/\\\\]|$)/g, '');"
            + "    var filePath = pathMod.join(publicDir, safePath);"
            + "    if (!filePath.startsWith(publicDir)) {"
            + "      res.writeHead(403); res.end('Forbidden'); return;"
            + "    }"
            + "    try {"
            + "      var stat = fs.statSync(filePath);"
            + "      if (stat.isDirectory()) {"
            + "        filePath = pathMod.join(filePath, 'index.html');"
            + "        stat = fs.statSync(filePath);"
            + "      }"
            + "      if (stat.isFile()) {"
            + "        var ext = pathMod.extname(filePath).toLowerCase();"
            + "        var ct = mimeTypes[ext] || 'application/octet-stream';"
            + "        res.writeHead(200, {'Content-Type': ct, 'Content-Length': stat.size});"
            + "        fs.createReadStream(filePath).pipe(res);"
            + "        return;"
            + "      }"
            + "    } catch(e) {}"
            + "  }"
            + "  inFlight++;"
            + "  var chunks = [];"
            + "  req.on('data', function(chunk) { chunks.push(chunk); });"
            + "  req.on('end', function() {"
            + "    req.__body = chunks.length > 0 ? Buffer.concat(chunks).toString() : null;"
            + "    $rt_startThread(function() {"
            + "      try { callback(req, res); } finally {"
            + "        inFlight--;"
            + "        if (shuttingDown && inFlight <= 0) {"
            + "          console.log('All in-flight requests completed, exiting.');"
            + "          process.exit(0);"
            + "        }"
            + "      }"
            + "    });"
            + "  });"
            + "});"
            + "process.on('SIGTERM', function() {"
            + "  console.log('SIGTERM received, starting graceful shutdown...');"
            + "  shuttingDown = true;"
            + "  server.close(function() {"
            + "    console.log('Server closed, no new connections.');"
            + "    if (inFlight <= 0) { process.exit(0); }"
            + "  });"
            + "  setTimeout(function() {"
            + "    console.log('Graceful shutdown timeout, forcing exit.');"
            + "    process.exit(1);"
            + "  }, 10000);"
            + "});"
            + "server.listen(port, '0.0.0.0', function() {"
            + "  console.log('Cloud Run server listening on port ' + port);"
            + "});")
    private static native void startServer(RequestCallback callback, int port);

    @JSBody(params = {"name"}, script = "return process.env[name] || '';")
    private static native String getEnv(String name);

    @JSBody(params = {"req"}, script = "return req.method || 'GET';")
    private static native String getMethod(JSObject req);

    @JSBody(params = {"req"}, script = "return require('url').parse(req.url, true).pathname || '/';")
    private static native String getPath(JSObject req);

    @JSBody(params = {"req"}, script = "return req.__body || null;")
    private static native String getBody(JSObject req);

    @JSBody(params = {"req"}, script = "return req.headers || {};")
    private static native JSObject getHeaders(JSObject req);

    @JSBody(params = {"req"}, script = ""
            + "var parsed = require('url').parse(req.url, true);"
            + "return parsed.query || {};")
    private static native JSObject getQueryParams(JSObject req);

    @JSBody(params = {"obj"}, script = ""
            + "var keys = Object.keys(obj || {});"
            + "var result = [];"
            + "for (var i = 0; i < keys.length; i++) {"
            + "  result.push(keys[i]);"
            + "  result.push(String(obj[keys[i]]));"
            + "}"
            + "return result;")
    private static native String[] jsObjectToEntries(JSObject obj);

    @JSBody(params = {"res", "statusCode", "headersArr", "body"}, script = ""
            + "for (var i = 0; i < headersArr.length; i += 2) {"
            + "  res.setHeader(headersArr[i], headersArr[i + 1]);"
            + "}"
            + "res.writeHead(statusCode);"
            + "res.end(body || '');")
    private static native void sendResponse(JSObject res, int statusCode, String[] headersArr, String body);

    @JSBody(params = {"res", "statusCode", "headersArr", "base64Body"}, script = ""
            + "for (var i = 0; i < headersArr.length; i += 2) {"
            + "  res.setHeader(headersArr[i], headersArr[i + 1]);"
            + "}"
            + "res.writeHead(statusCode);"
            + "res.end(Buffer.from(base64Body, 'base64'));")
    private static native void sendBinaryResponse(JSObject res, int statusCode, String[] headersArr, String base64Body);

    @JSBody(script = "return typeof process !== 'undefined' && typeof process.hrtime === 'function'"
            + " ? Number(process.hrtime.bigint()) / 1e6 : Date.now();")
    private static native double now();

    static void handleRequest(JSObject req, JSObject res) {
        double startTime = now();
        String httpMethod = null;
        String path = null;
        try {
            httpMethod = getMethod(req);
            path = getPath(req);
            String body = getBody(req);

            Map<String, String> headers = entriesToMap(jsObjectToEntries(getHeaders(req)));
            Map<String, String> queryParams = entriesToMap(jsObjectToEntries(getQueryParams(req)));

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
            if (response.getBodyBytes() != null) {
                String base64 = Base64.getEncoder().encodeToString(response.getBodyBytes());
                sendBinaryResponse(res, response.getStatusCode(), headersArr, base64);
            } else {
                sendResponse(res, response.getStatusCode(), headersArr, response.getBody());
            }
        } catch (Exception e) {
            double duration = now() - startTime;
            logger.error("request failed", e);
            logger.error("request error context",
                    "{\"method\":\"" + (httpMethod != null ? httpMethod : "unknown")
                    + "\",\"path\":\"" + (path != null ? path : "unknown")
                    + "\",\"duration_ms\":" + Math.round(duration) + "}");
            Sentry.captureException(e);
            sendResponse(res, 500, new String[0], "{\"error\":\"" + e.getMessage() + "\"}");
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
