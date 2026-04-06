package ca.weblite.teavmlambda.impl.js.gcs;

import ca.weblite.teavmlambda.api.objectstore.ObjectStoreClient;
import org.teavm.jso.JSObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Google Cloud Storage implementation of ObjectStoreClient.
 */
public class GcsObjectStoreClient implements ObjectStoreClient {

    private final JSObject storage;

    GcsObjectStoreClient(JSObject storage) {
        this.storage = storage;
    }

    @Override
    public void putObject(String bucket, String key, String data, String contentType) {
        GcsAsyncBridge.awaitVoid(
                GcsJsBridge.putObject(storage, bucket, key, data, contentType));
    }

    @Override
    public String getObject(String bucket, String key) {
        try {
            JSObject result = GcsAsyncBridge.await(
                    GcsJsBridge.getObject(storage, bucket, key));
            return GcsJsBridge.jsToString(result);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("No such object")) {
                return null;
            }
            throw e;
        }
    }

    @Override
    public void deleteObject(String bucket, String key) {
        GcsAsyncBridge.awaitVoid(
                GcsJsBridge.deleteObject(storage, bucket, key));
    }

    @Override
    public List<String> listObjects(String bucket, String prefix) {
        JSObject result = GcsAsyncBridge.await(
                GcsJsBridge.listObjects(storage, bucket, prefix));

        List<String> keys = new ArrayList<>();
        int len = GcsJsBridge.arrayLength(result);
        for (int i = 0; i < len; i++) {
            keys.add(GcsJsBridge.arrayGetString(result, i));
        }
        return keys;
    }

    @Override
    public boolean objectExists(String bucket, String key) {
        JSObject result = GcsAsyncBridge.await(
                GcsJsBridge.objectExists(storage, bucket, key));
        return GcsJsBridge.getBooleanProperty(result, "exists");
    }

    @Override
    public void putObjectBytes(String bucket, String key, byte[] data, String contentType) {
        JSObject buffer = GcsJsBridge.toNodeBuffer(data);
        GcsAsyncBridge.awaitVoid(
                GcsJsBridge.putObjectBytes(storage, bucket, key, buffer, contentType));
    }

    @Override
    public byte[] getObjectBytes(String bucket, String key) {
        try {
            JSObject result = GcsAsyncBridge.await(
                    GcsJsBridge.getObjectBytes(storage, bucket, key));
            return GcsJsBridge.fromNodeBuffer(result);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().contains("No such object")) {
                return null;
            }
            throw e;
        }
    }
}
