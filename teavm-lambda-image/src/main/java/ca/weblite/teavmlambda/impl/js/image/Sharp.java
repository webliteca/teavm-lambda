package ca.weblite.teavmlambda.impl.js.image;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSPromise;

/**
 * JSO wrapper around the Node.js sharp image processing library.
 * <p>
 * Sharp operations are chainable and produce a Promise via {@link #toBuffer(Sharp)}.
 */
public abstract class Sharp implements JSObject {

    @JSBody(params = {"buffer"}, script =
            "var sharp = require('sharp');"
            + "return sharp(buffer);")
    public static native Sharp create(JSObject buffer);

    @JSBody(params = {"instance", "width", "height"}, script =
            "return instance.resize(width, height, { fit: 'inside' });")
    public static native Sharp resizeFit(Sharp instance, int width, int height);

    @JSBody(params = {"instance", "width", "height"}, script =
            "return instance.resize(width, height, { fit: 'fill' });")
    public static native Sharp resizeFill(Sharp instance, int width, int height);

    @JSBody(params = {"instance"}, script =
            "return instance.png();")
    public static native Sharp png(Sharp instance);

    @JSBody(params = {"instance"}, script =
            "return instance.jpeg();")
    public static native Sharp jpeg(Sharp instance);

    @JSBody(params = {"instance", "quality"}, script =
            "return instance.jpeg({ quality: quality });")
    public static native Sharp jpegWithQuality(Sharp instance, int quality);

    @JSBody(params = {"instance"}, script =
            "return instance.webp();")
    public static native Sharp webp(Sharp instance);

    @JSBody(params = {"instance", "quality"}, script =
            "return instance.webp({ quality: quality });")
    public static native Sharp webpWithQuality(Sharp instance, int quality);

    @JSBody(params = {"instance"}, script =
            "return instance.toBuffer();")
    public static native JSPromise<JSObject> toBuffer(Sharp instance);
}
