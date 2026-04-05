package ca.weblite.teavmlambda.api.nosqldb;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

/**
 * JavaScript interop utilities shared across NoSQL provider implementations.
 */
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

    @JSBody(params = {}, script = "return {};")
    public static native JSObject newObject();

    @JSBody(params = {"obj", "key", "value"}, script = "obj[key] = value;")
    public static native void setProperty(JSObject obj, String key, String value);

    @JSBody(params = {"obj", "key", "value"}, script = "obj[key] = value;")
    public static native void setIntProperty(JSObject obj, String key, int value);

    @JSBody(params = {"obj", "key", "value"}, script = "obj[key] = value;")
    public static native void setObjectProperty(JSObject obj, String key, JSObject value);

    @JSBody(params = {"arr"}, script = "return arr.length;")
    public static native int arrayLength(JSObject arr);

    @JSBody(params = {"arr", "index"}, script = "return arr[index];")
    public static native JSObject arrayGet(JSObject arr, int index);

    @JSBody(params = {"obj", "key"}, script = "return obj[key];")
    public static native JSObject getObjectProperty(JSObject obj, String key);
}
