package ca.weblite.teavmlambda.api.middleware;

import ca.weblite.teavmlambda.api.Middleware;
import ca.weblite.teavmlambda.api.MiddlewareChain;
import ca.weblite.teavmlambda.api.Request;
import ca.weblite.teavmlambda.api.Response;
import ca.weblite.teavmlambda.api.compression.Compressor;
import ca.weblite.teavmlambda.api.compression.CompressorFactory;

/**
 * Middleware that compresses response bodies based on the {@code Accept-Encoding} header.
 * <p>
 * Supports gzip and deflate encodings. Only compresses responses with text-based
 * content types and bodies larger than the minimum threshold.
 * <p>
 * Requires a compression provider: add {@code teavm-lambda-compression} (JS)
 * or {@code teavm-lambda-compression-jvm} (JVM) to your dependencies.
 */
public final class CompressionMiddleware implements Middleware {

    private static final int MIN_SIZE = 256;
    private final Compressor compressor;

    public CompressionMiddleware() {
        this.compressor = CompressorFactory.create();
    }

    public CompressionMiddleware(Compressor compressor) {
        this.compressor = compressor;
    }

    @Override
    public Response handle(Request request, MiddlewareChain chain) {
        Response response = chain.next(request);

        String body = response.getBody();
        if (body == null || body.length() < MIN_SIZE) {
            return response;
        }

        String acceptEncoding = request.getHeaders().get("accept-encoding");
        if (acceptEncoding == null) {
            acceptEncoding = request.getHeaders().get("Accept-Encoding");
        }
        if (acceptEncoding == null) {
            return response;
        }

        // Check content type — only compress text-based types
        String contentType = response.getHeaders().get("Content-Type");
        if (contentType == null) {
            contentType = response.getHeaders().get("content-type");
        }
        if (contentType != null && !isCompressible(contentType)) {
            return response;
        }

        if (acceptEncoding.contains("gzip")) {
            String compressed = compressor.gzip(body);
            return response
                    .header("Content-Encoding", "gzip")
                    .header("Vary", "Accept-Encoding")
                    .body(compressed);
        }

        if (acceptEncoding.contains("deflate")) {
            String compressed = compressor.deflate(body);
            return response
                    .header("Content-Encoding", "deflate")
                    .header("Vary", "Accept-Encoding")
                    .body(compressed);
        }

        return response;
    }

    private static boolean isCompressible(String contentType) {
        return contentType.startsWith("text/")
                || contentType.contains("json")
                || contentType.contains("xml")
                || contentType.contains("javascript");
    }
}
