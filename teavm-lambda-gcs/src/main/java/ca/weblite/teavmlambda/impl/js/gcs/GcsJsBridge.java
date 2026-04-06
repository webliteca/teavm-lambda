package ca.weblite.teavmlambda.impl.js.gcs;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSPromise;

/**
 * Low-level JavaScript interop for the Google Cloud Storage Node.js SDK
 * ({@code @google-cloud/storage}).
 */
final class GcsJsBridge {

    private GcsJsBridge() {
    }

    @JSBody(params = {"projectId"}, script =
            "var Storage = require('@google-cloud/storage').Storage;"
            + "var config = {};"
            + "if (projectId) { config.projectId = projectId; }"
            + "return new Storage(config);")
    static native JSObject createClient(String projectId);

    @JSBody(params = {"storage", "bucket", "key", "data", "contentType"}, script =
            "return storage.bucket(bucket).file(key)"
            + ".save(data, { contentType: contentType });")
    static native JSPromise<JSObject> putObject(JSObject storage, String bucket, String key,
            String data, String contentType);

    @JSBody(params = {"storage", "bucket", "key"}, script =
            "return storage.bucket(bucket).file(key).download()"
            + ".then(function(data) { return data[0].toString('utf-8'); });")
    static native JSPromise<JSObject> getObject(JSObject storage, String bucket, String key);

    @JSBody(params = {"storage", "bucket", "key"}, script =
            "return storage.bucket(bucket).file(key).delete();")
    static native JSPromise<JSObject> deleteObject(JSObject storage, String bucket, String key);

    @JSBody(params = {"storage", "bucket", "prefix"}, script =
            "var opts = {};"
            + "if (prefix) { opts.prefix = prefix; }"
            + "return storage.bucket(bucket).getFiles(opts)"
            + ".then(function(data) {"
            + "  return data[0].map(function(f) { return f.name; });"
            + "});")
    static native JSPromise<JSObject> listObjects(JSObject storage, String bucket, String prefix);

    @JSBody(params = {"storage", "bucket", "key"}, script =
            "return storage.bucket(bucket).file(key).exists()"
            + ".then(function(data) { return { exists: data[0] }; });")
    static native JSPromise<JSObject> objectExists(JSObject storage, String bucket, String key);

    @JSBody(params = {"obj"}, script = "return String(obj);")
    static native String jsToString(JSObject obj);

    @JSBody(params = {"obj", "key"}, script = "return obj[key] ? true : false;")
    static native boolean getBooleanProperty(JSObject obj, String key);

    @JSBody(params = {"arr"}, script = "return arr ? arr.length : 0;")
    static native int arrayLength(JSObject arr);

    @JSBody(params = {"arr", "index"}, script = "return String(arr[index]);")
    static native String arrayGetString(JSObject arr, int index);
}
