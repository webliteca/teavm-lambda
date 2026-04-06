package ca.weblite.teavmlambda.impl.js.compression;

import ca.weblite.teavmlambda.api.compression.Compressor;
import org.teavm.jso.JSBody;

/**
 * Node.js zlib-based compression implementation.
 * <p>
 * Uses {@code zlib.gzipSync()} and {@code zlib.deflateSync()} via JSO interop.
 * Returns the compressed data as a base64-encoded string suitable for
 * use as an HTTP response body with binary transport.
 */
public final class JsCompressor implements Compressor {

    @JSBody(params = {"data"}, script = ""
            + "var zlib = require('zlib');"
            + "var buf = zlib.gzipSync(Buffer.from(data, 'utf8'));"
            + "return buf.toString('binary');")
    private static native String gzipNative(String data);

    @JSBody(params = {"data"}, script = ""
            + "var zlib = require('zlib');"
            + "var buf = zlib.deflateSync(Buffer.from(data, 'utf8'));"
            + "return buf.toString('binary');")
    private static native String deflateNative(String data);

    @Override
    public String gzip(String data) {
        return gzipNative(data);
    }

    @Override
    public String deflate(String data) {
        return deflateNative(data);
    }
}
