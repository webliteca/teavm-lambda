package ca.weblite.teavmlambda.impl.js.image;

import ca.weblite.teavmlambda.api.image.ImageFormat;
import ca.weblite.teavmlambda.api.image.ImageOptions;
import ca.weblite.teavmlambda.api.image.ImageProcessor;
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSPromise;

/**
 * Node.js/TeaVM implementation of {@link ImageProcessor} using the sharp library.
 */
public class JsImageProcessor implements ImageProcessor {

    @Override
    public byte[] process(byte[] input, ImageOptions options) {
        JSObject buffer = JsImageUtil.toNodeBuffer(input);
        Sharp pipeline = Sharp.create(buffer);

        if (options.getWidth() != null || options.getHeight() != null) {
            int width = options.getWidth() != null ? options.getWidth() : 0;
            int height = options.getHeight() != null ? options.getHeight() : 0;
            if (options.isPreserveAspectRatio()) {
                pipeline = Sharp.resizeFit(pipeline, width > 0 ? width : height, height > 0 ? height : width);
            } else {
                pipeline = Sharp.resizeFill(pipeline, width, height);
            }
        }

        ImageFormat format = options.getFormat();
        if (format != null) {
            Integer quality = options.getQuality();
            switch (format) {
                case PNG:
                    pipeline = Sharp.png(pipeline);
                    break;
                case JPEG:
                    pipeline = quality != null
                            ? Sharp.jpegWithQuality(pipeline, quality)
                            : Sharp.jpeg(pipeline);
                    break;
                case WEBP:
                    pipeline = quality != null
                            ? Sharp.webpWithQuality(pipeline, quality)
                            : Sharp.webp(pipeline);
                    break;
            }
        } else if (options.getQuality() != null) {
            // Quality without explicit format: apply to jpeg by default
            pipeline = Sharp.jpegWithQuality(pipeline, options.getQuality());
        }

        JSObject resultBuffer = awaitBuffer(Sharp.toBuffer(pipeline));
        return JsImageUtil.fromNodeBuffer(resultBuffer);
    }

    @Async
    private static native JSObject awaitBuffer(JSPromise<JSObject> promise);

    private static void awaitBuffer(JSPromise<JSObject> promise, AsyncCallback<JSObject> callback) {
        thenResolve(promise,
                value -> callback.complete(value),
                err -> callback.error(new RuntimeException(jsErrorToString(err))));
    }

    @JSBody(params = {"promise", "resolve", "reject"}, script =
            "promise.then(function(value) { resolve(value); })"
            + ".catch(function(err) { reject(err); });")
    private static native void thenResolve(JSObject promise, ResolveCallback resolve, RejectCallback reject);

    @JSBody(params = {"err"}, script = "return String(err && err.message ? err.message : err);")
    private static native String jsErrorToString(JSObject err);

    @JSFunctor
    interface ResolveCallback extends JSObject {
        void resolve(JSObject value);
    }

    @JSFunctor
    interface RejectCallback extends JSObject {
        void reject(JSObject error);
    }
}
