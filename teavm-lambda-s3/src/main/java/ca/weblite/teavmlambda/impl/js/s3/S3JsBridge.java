package ca.weblite.teavmlambda.impl.js.s3;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSPromise;

/**
 * Low-level JavaScript interop for the AWS SDK v3 S3 Client.
 * Uses @aws-sdk/client-s3 for object storage operations.
 */
final class S3JsBridge {

    private S3JsBridge() {
    }

    @JSBody(params = {"region", "endpoint", "forcePathStyle"}, script =
            "var s3 = require('@aws-sdk/client-s3');"
            + "var config = { region: region };"
            + "if (endpoint) { config.endpoint = endpoint; config.forcePathStyle = forcePathStyle; }"
            + "return new s3.S3Client(config);")
    static native JSObject createClient(String region, String endpoint, boolean forcePathStyle);

    @JSBody(params = {"client", "bucket", "key", "body", "contentType"}, script =
            "var s3 = require('@aws-sdk/client-s3');"
            + "return client.send(new s3.PutObjectCommand({"
            + "  Bucket: bucket, Key: key, Body: body, ContentType: contentType"
            + "}));")
    static native JSPromise<JSObject> putObject(JSObject client, String bucket, String key,
            String body, String contentType);

    @JSBody(params = {"client", "bucket", "key"}, script =
            "var s3 = require('@aws-sdk/client-s3');"
            + "return client.send(new s3.GetObjectCommand({"
            + "  Bucket: bucket, Key: key"
            + "})).then(function(resp) {"
            + "  return resp.Body.transformToString();"
            + "});")
    static native JSPromise<JSObject> getObject(JSObject client, String bucket, String key);

    @JSBody(params = {"client", "bucket", "key"}, script =
            "var s3 = require('@aws-sdk/client-s3');"
            + "return client.send(new s3.DeleteObjectCommand({"
            + "  Bucket: bucket, Key: key"
            + "}));")
    static native JSPromise<JSObject> deleteObject(JSObject client, String bucket, String key);

    @JSBody(params = {"client", "bucket", "prefix"}, script =
            "var s3 = require('@aws-sdk/client-s3');"
            + "var params = { Bucket: bucket };"
            + "if (prefix) { params.Prefix = prefix; }"
            + "return client.send(new s3.ListObjectsV2Command(params));")
    static native JSPromise<JSObject> listObjects(JSObject client, String bucket, String prefix);

    @JSBody(params = {"client", "bucket", "key"}, script =
            "var s3 = require('@aws-sdk/client-s3');"
            + "return client.send(new s3.HeadObjectCommand({"
            + "  Bucket: bucket, Key: key"
            + "})).then(function() { return { exists: true }; })"
            + ".catch(function(e) {"
            + "  if (e.name === 'NotFound' || e.$metadata && e.$metadata.httpStatusCode === 404) {"
            + "    return { exists: false };"
            + "  }"
            + "  throw e;"
            + "});")
    static native JSPromise<JSObject> headObject(JSObject client, String bucket, String key);

    @JSBody(params = {"obj"}, script = "return String(obj);")
    static native String jsToString(JSObject obj);

    @JSBody(params = {"obj", "key"}, script = "return obj[key];")
    static native JSObject getProperty(JSObject obj, String key);

    @JSBody(params = {"obj", "key"}, script = "return obj[key] != null;")
    static native boolean hasProperty(JSObject obj, String key);

    @JSBody(params = {"obj", "key"}, script = "return obj[key] ? true : false;")
    static native boolean getBooleanProperty(JSObject obj, String key);

    @JSBody(params = {"arr"}, script = "return arr ? arr.length : 0;")
    static native int arrayLength(JSObject arr);

    @JSBody(params = {"arr", "index"}, script = "return arr[index];")
    static native JSObject arrayGet(JSObject arr, int index);

    @JSBody(params = {"obj", "key"}, script = "return String(obj[key]);")
    static native String getStringProperty(JSObject obj, String key);
}
