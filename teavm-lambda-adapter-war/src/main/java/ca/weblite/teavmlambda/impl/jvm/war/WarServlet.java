package ca.weblite.teavmlambda.impl.jvm.war;

import ca.weblite.teavmlambda.api.Request;
import ca.weblite.teavmlambda.api.Response;
import ca.weblite.teavmlambda.api.Router;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base servlet for WAR deployment of teavm-lambda applications.
 * <p>
 * Translates between the Servlet API and the teavm-lambda {@link Request}/{@link Response}
 * model.  Subclass this servlet and implement {@link #createRouter()} to provide
 * your application's router:
 * <pre>
 * {@literal @}WebServlet(urlPatterns = "/*")
 * public class MyServlet extends WarServlet {
 *     {@literal @}Override
 *     protected Router createRouter() {
 *         Container container = new GeneratedContainer();
 *         return new GeneratedRouter(container);
 *     }
 * }
 * </pre>
 * <p>
 * The servlet can be deployed to any Servlet 6.0+ container (Tomcat 10.1+,
 * TomEE 10+, Jetty 12+, etc.).
 */
public abstract class WarServlet extends HttpServlet {

    private static final Logger logger = Logger.getLogger(WarServlet.class.getName());

    private transient Router router;

    /**
     * Creates the application router.
     * Called once during {@link #init()}.
     *
     * @return the router that handles all requests
     */
    protected abstract Router createRouter();

    @Override
    public void init() throws ServletException {
        super.init();
        router = createRouter();
        if (router == null) {
            throw new ServletException("createRouter() must not return null");
        }
        logger.info("WarServlet initialized");
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        long startTime = System.nanoTime();
        String httpMethod = req.getMethod();
        String path = req.getPathInfo();
        if (path == null || path.isEmpty()) {
            path = req.getServletPath();
        }
        if (path == null || path.isEmpty()) {
            path = "/";
        }

        try {
            Map<String, String> headers = new HashMap<>();
            var headerNames = req.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String name = headerNames.nextElement();
                headers.put(name.toLowerCase(), req.getHeader(name));
            }

            Map<String, String> queryParams = parseQueryParams(req.getQueryString());

            String body = null;
            byte[] bytes = req.getInputStream().readAllBytes();
            if (bytes.length > 0) {
                body = new String(bytes, StandardCharsets.UTF_8);
            }

            Request request = new Request(httpMethod, path, headers, queryParams, body);
            Response response = router.route(request);

            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            logger.info(String.format("request completed method=%s path=%s status=%d duration_ms=%d",
                    httpMethod, path, response.getStatusCode(), durationMs));

            for (Map.Entry<String, String> header : response.getHeaders().entrySet()) {
                resp.setHeader(header.getKey(), header.getValue());
            }

            resp.setStatus(response.getStatusCode());

            if (response.getBody() != null && !response.getBody().isEmpty()) {
                byte[] responseBody = response.getBody().getBytes(StandardCharsets.UTF_8);
                resp.setContentLength(responseBody.length);
                try (OutputStream os = resp.getOutputStream()) {
                    os.write(responseBody);
                }
            }
        } catch (Exception e) {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            logger.log(Level.SEVERE, String.format("request failed method=%s path=%s duration_ms=%d",
                    httpMethod, path, durationMs), e);
            sendError(resp, 500, e.getMessage());
        }
    }

    private static void sendError(HttpServletResponse resp, int status, String message) {
        try {
            String body = "{\"error\":\"" + (message != null ? message : "Internal Server Error") + "\"}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            resp.setStatus(status);
            resp.setContentType("application/json");
            resp.setContentLength(bytes.length);
            try (OutputStream os = resp.getOutputStream()) {
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
                String key = URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
                params.put(key, value);
            } else {
                params.put(URLDecoder.decode(pair, StandardCharsets.UTF_8), "");
            }
        }
        return params;
    }
}
