package io.teavmlambda.db;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

public final class JsUtil {

    private JsUtil() {
    }

    @JSBody(params = {"obj", "key"}, script = "return String(obj[key]);")
    public static native String getStringProperty(JSObject obj, String key);

    @JSBody(params = {"obj", "key"}, script = "return obj[key] != null ? Number(obj[key]) : 0;")
    public static native int getIntProperty(JSObject obj, String key);

    @JSBody(params = {"obj", "key"}, script = "return obj[key] != null ? Number(obj[key]) : 0;")
    public static native double getDoubleProperty(JSObject obj, String key);

    @JSBody(params = {"obj", "key"}, script = "return obj[key] != null;")
    public static native boolean hasProperty(JSObject obj, String key);

    @JSBody(params = {"obj"}, script = "return JSON.stringify(obj);")
    public static native String toJson(JSObject obj);

    @JSBody(params = {"json"}, script = "return JSON.parse(json);")
    public static native JSObject parseJson(String json);

    @JSBody(params = {"values"}, script = "return values;")
    public static native JSObject toJsArray(String[] values);
}
