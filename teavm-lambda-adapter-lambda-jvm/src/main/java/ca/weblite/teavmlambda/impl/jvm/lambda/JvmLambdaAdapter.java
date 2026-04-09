package ca.weblite.teavmlambda.impl.jvm.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import ca.weblite.teavmlambda.api.Request;
import ca.weblite.teavmlambda.api.Response;
import ca.weblite.teavmlambda.api.Router;

import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AWS Lambda adapter for JVM deployment.
 * <p>
 * Implements the AWS Lambda Java runtime {@link RequestHandler} interface,
 * translating API Gateway proxy events to teavm-lambda {@link Request}/{@link Response}.
 * <p>
 * Usage: extend this class and pass your Router to the constructor:
 * <pre>
 * public class MyHandler extends JvmLambdaAdapter {
 *     public MyHandler() {
 *         super(createRouter());
 *     }
 *     private static Router createRouter() {
 *         // build and return your GeneratedRouter
 *     }
 * }
 * </pre>
 */
public class JvmLambdaAdapter
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger logger = Logger.getLogger(JvmLambdaAdapter.class.getName());

    private final Router router;

    public JvmLambdaAdapter(Router router) {
        this.router = router;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(
            APIGatewayProxyRequestEvent event, Context context) {
        long startTime = System.nanoTime();
        String httpMethod = event.getHttpMethod();
        String path = event.getPath();

        try {
            Map<String, String> headers = event.getHeaders();
            if (headers == null) headers = Collections.emptyMap();
            Map<String, String> queryParams = event.getQueryStringParameters();
            if (queryParams == null) queryParams = Collections.emptyMap();

            Request request = new Request(httpMethod, path, headers, queryParams, event.getBody());
            Response response = router.route(request);

            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            logger.info(String.format("request completed method=%s path=%s status=%d duration_ms=%d",
                    httpMethod, path, response.getStatusCode(), durationMs));

            APIGatewayProxyResponseEvent apiResponse = new APIGatewayProxyResponseEvent();
            apiResponse.setStatusCode(response.getStatusCode());
            apiResponse.setHeaders(new HashMap<>(response.getHeaders()));
            if (response.getBodyBytes() != null) {
                apiResponse.setBody(Base64.getEncoder().encodeToString(response.getBodyBytes()));
                apiResponse.setIsBase64Encoded(true);
            } else {
                apiResponse.setBody(response.getBody() != null ? response.getBody() : "");
            }
            return apiResponse;
        } catch (Exception e) {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            logger.log(Level.SEVERE, String.format("request failed method=%s path=%s duration_ms=%d",
                    httpMethod != null ? httpMethod : "unknown",
                    path != null ? path : "unknown",
                    durationMs), e);

            APIGatewayProxyResponseEvent errorResponse = new APIGatewayProxyResponseEvent();
            errorResponse.setStatusCode(500);
            errorResponse.setHeaders(Map.of("Content-Type", "application/json"));
            errorResponse.setBody("{\"error\":\"" + e.getMessage() + "\"}");
            return errorResponse;
        }
    }
}
