package ca.weblite.teavmlambda.impl.js.s3;

import ca.weblite.teavmlambda.api.objectstore.ObjectStoreClient;
import org.teavm.jso.JSObject;

import java.util.ArrayList;
import java.util.List;

/**
 * AWS S3 implementation of ObjectStoreClient using the AWS SDK v3 for JavaScript.
 */
public class S3ObjectStoreClient implements ObjectStoreClient {

    private final JSObject s3Client;

    S3ObjectStoreClient(JSObject s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public void putObject(String bucket, String key, String data, String contentType) {
        S3AsyncBridge.awaitVoid(
                S3JsBridge.putObject(s3Client, bucket, key, data, contentType));
    }

    @Override
    public String getObject(String bucket, String key) {
        try {
            JSObject result = S3AsyncBridge.await(
                    S3JsBridge.getObject(s3Client, bucket, key));
            return S3JsBridge.jsToString(result);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && (e.getMessage().contains("NoSuchKey")
                    || e.getMessage().contains("The specified key does not exist"))) {
                return null;
            }
            throw e;
        }
    }

    @Override
    public void deleteObject(String bucket, String key) {
        S3AsyncBridge.awaitVoid(
                S3JsBridge.deleteObject(s3Client, bucket, key));
    }

    @Override
    public List<String> listObjects(String bucket, String prefix) {
        JSObject result = S3AsyncBridge.await(
                S3JsBridge.listObjects(s3Client, bucket, prefix));

        List<String> keys = new ArrayList<>();
        if (S3JsBridge.hasProperty(result, "Contents")) {
            JSObject contents = S3JsBridge.getProperty(result, "Contents");
            int len = S3JsBridge.arrayLength(contents);
            for (int i = 0; i < len; i++) {
                JSObject item = S3JsBridge.arrayGet(contents, i);
                keys.add(S3JsBridge.getStringProperty(item, "Key"));
            }
        }
        return keys;
    }

    @Override
    public boolean objectExists(String bucket, String key) {
        JSObject result = S3AsyncBridge.await(
                S3JsBridge.headObject(s3Client, bucket, key));
        return S3JsBridge.getBooleanProperty(result, "exists");
    }
}
