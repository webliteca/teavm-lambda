package ca.weblite.teavmlambda.impl.jvm.gcs;

import ca.weblite.teavmlambda.api.objectstore.ObjectStoreClient;
import com.google.cloud.storage.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * JVM implementation of ObjectStoreClient using the Google Cloud Storage Java SDK.
 */
public class GoogleGcsObjectStoreClient implements ObjectStoreClient {

    private final Storage storage;

    GoogleGcsObjectStoreClient(Storage storage) {
        this.storage = storage;
    }

    @Override
    public void putObject(String bucket, String key, String data, String contentType) {
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucket, key))
                .setContentType(contentType)
                .build();
        storage.create(blobInfo, data.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String getObject(String bucket, String key) {
        Blob blob = storage.get(BlobId.of(bucket, key));
        if (blob == null || !blob.exists()) {
            return null;
        }
        return new String(blob.getContent(), StandardCharsets.UTF_8);
    }

    @Override
    public void deleteObject(String bucket, String key) {
        storage.delete(BlobId.of(bucket, key));
    }

    @Override
    public List<String> listObjects(String bucket, String prefix) {
        Storage.BlobListOption[] options = (prefix != null && !prefix.isEmpty())
                ? new Storage.BlobListOption[]{Storage.BlobListOption.prefix(prefix)}
                : new Storage.BlobListOption[0];

        List<String> keys = new ArrayList<>();
        for (Blob blob : storage.list(bucket, options).iterateAll()) {
            keys.add(blob.getName());
        }
        return keys;
    }

    @Override
    public boolean objectExists(String bucket, String key) {
        Blob blob = storage.get(BlobId.of(bucket, key));
        return blob != null && blob.exists();
    }

    @Override
    public void putObjectBytes(String bucket, String key, byte[] data, String contentType) {
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucket, key))
                .setContentType(contentType)
                .build();
        storage.create(blobInfo, data);
    }

    @Override
    public byte[] getObjectBytes(String bucket, String key) {
        Blob blob = storage.get(BlobId.of(bucket, key));
        if (blob == null || !blob.exists()) {
            return null;
        }
        return blob.getContent();
    }
}
