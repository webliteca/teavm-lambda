package ca.weblite.teavmlambda.impl.js.image;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

/**
 * Utility methods for converting between Java byte arrays and Node.js Buffers.
 */
public class JsImageUtil {

    private JsImageUtil() {
    }

    @JSBody(params = {"bytes"}, script = "return Buffer.from(bytes);")
    public static native JSObject toNodeBuffer(byte[] bytes);

    @JSBody(params = {"buffer"}, script =
            "var arr = new Int8Array(buffer.buffer, buffer.byteOffset, buffer.length);"
            + "return Array.prototype.slice.call(arr);")
    public static native byte[] fromNodeBuffer(JSObject buffer);
}
