package io.teavmlambda.adapter.cloudrun.jvm;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.teavmlambda.core.Request;
import io.teavmlambda.core.Response;
import io.teavmlambda.core.Router;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cloud Run / standalone HTTP server adapter for JVM deployment.
 * <p>
 * Uses the JDK built-in {@link com.sun.net.httpserver.HttpServer} (zero external dependencies).
 * Serves static files from a {@code public/} directory if it exists, then falls through
 * to the Java router for API requests.
 */
public final class JvmCloudRunAdapter {

    private static final Logger logger = Logger.getLogger(JvmCloudRunAdapter.class.getName());

    private static final Map<String, String> MIME_TYPES = Map.ofEntries(
            Map.entry(".html", "text/html"),
            Map.entry(".css", "text/css"),
            Map.entry(".js", "application/javascript"),
            Map.entry(".json", "application/json"),
            Map.entry(".png", "image/png"),
            Map.entry(".jpg", "image/jpeg"),
            Map.entry(".jpeg", "image/jpeg"),
            Map.entry(".gif", "image/gif"),
            Map.entry(".svg", "image/svg+xml"),
            Map.entry(".ico", "image/x-icon"),
            Map.entry(".woff", "font/woff"),
            Map.entry(".woff2", "font/woff2"),
            Map.entry(".ttf", "font/ttf"),
            Map.entry(".txt", "text/plain"),
            Map.entry(".xml", "application/xml"),
            Map.entry(".webp", "image/webp"),
            Map.entry(".map", "application/json")
    );

    private JvmCloudRunAdapter() {
    }

    /**
     * Starts the HTTP server with the given router.
     * Reads the port from the {@code PORT} environment variable (default 8080).
     */
    public static void start(Router router) {
        String portStr = System.getenv("PORT");
        int port = (portStr != null && !portStr.isEmpty()) ? Integer.parseInt(portStr) : 8080;
        start(router, port);
    }

    /**
     * Starts the HTTP server with the given router on the specified port.
     */
    public static void start(Router router, int port) {
        try {
            Path publicDir = Path.of("public");
            boolean hasPublicDir = Files.isDirectory(publicDir);

            HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
            server.createContext("/", exchange -> {
                try {
                    if (hasPublicDir && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                        if (tryServeStaticFile(exchange, publicDir)) {
                            return;
                        }
                    }
                    handleApiRequest(exchange, router);
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Unhandled error", e);
                    sendError(exchange, 500, e.getMessage());
                }
            });
            server.start();
            logger.info("Cloud Run server listening on port " + port);
        } catch (IOException e) {
            throw new RuntimeException("Failed to start HTTP server", e);
        }
    }

    private static boolean tryServeStaticFile(HttpExchange exchange, Path publicDir) throws IOException {
        String path = exchange.getRequestURI().getPath();
        Path normalized = publicDir.resolve(path.substring(1)).normalize();
        if (!normalized.startsWith(publicDir)) {
            return false; // path traversal attempt
        }

        if (Files.isDirectory(normalized)) {
            normalized = normalized.resolve("index.html");
        }

        if (!Files.isRegularFile(normalized)) {
            return false;
        }

        String fileName = normalized.getFileName().toString();
        int dotIdx = fileName.lastIndexOf('.');
        String ext = dotIdx >= 0 ? fileName.substring(dotIdx).toLowerCase() : "";
        String contentType = MIME_TYPES.getOrDefault(ext, "application/octet-stream");

        byte[] content = Files.readAllBytes(normalized);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, content.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(content);
        }
        return true;
    }

    private static void handleApiRequest(HttpExchange exchange, Router router) throws IOException {
        long startTime = System.nanoTime();
        String httpMethod = exchange.getRequestMethod();
        URI uri = exchange.getRequestURI();
        String path = uri.getPath();

        try {
            String body = null;
            try (InputStream is = exchange.getRequestBody()) {
                byte[] bytes = is.readAllBytes();
                if (bytes.length > 0) {
                    body = new String(bytes, StandardCharsets.UTF_8);
                }
            }

            Map<String, String> headers = new HashMap<>();
            exchange.getRequestHeaders().forEach((key, values) -> {
                if (values != null && !values.isEmpty()) {
                    headers.put(key.toLowerCase(), values.get(0));
                }
            });

            Map<String, String> queryParams = parseQueryParams(uri.getRawQuery());

            Request request = new Request(httpMethod, path, headers, queryParams, body);
            Response response = router.route(request);

            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            logger.info(String.format("request completed method=%s path=%s status=%d duration_ms=%d",
                    httpMethod, path, response.getStatusCode(), durationMs));

            for (Map.Entry<String, String> header : response.getHeaders().entrySet()) {
                exchange.getResponseHeaders().set(header.getKey(), header.getValue());
            }

            byte[] responseBody = response.getBody() != null
                    ? response.getBody().getBytes(StandardCharsets.UTF_8)
                    : new byte[0];
            exchange.sendResponseHeaders(response.getStatusCode(),
                    responseBody.length > 0 ? responseBody.length : -1);
            if (responseBody.length > 0) {
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBody);
                }
            } else {
                exchange.getResponseBody().close();
            }
        } catch (Exception e) {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            logger.log(Level.SEVERE, String.format("request failed method=%s path=%s duration_ms=%d",
                    httpMethod, path, durationMs), e);
            sendError(exchange, 500, e.getMessage());
        }
    }

    private static void sendError(HttpExchange exchange, int status, String message) {
        try {
            String body = "{\"error\":\"" + (message != null ? message : "Internal Server Error") + "\"}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } catch (IOException ignored) {
            // response already committed
        }
    }

    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return params;
        }
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq >= 0) {
                String key = java.net.URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
                String value = java.net.URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
                params.put(key, value);
            } else {
                params.put(java.net.URLDecoder.decode(pair, StandardCharsets.UTF_8), "");
            }
        }
        return params;
    }
}
