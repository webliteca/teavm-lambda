package ca.weblite.teavmlambda.api.objectstore;

import java.util.List;

/**
 * Platform-neutral interface for object storage operations.
 *
 * <p>Implementations are provided by teavm-lambda-s3 (AWS S3) and
 * teavm-lambda-gcs (Google Cloud Storage).</p>
 */
public interface ObjectStoreClient {

    /**
     * Uploads an object to the store.
     *
     * @param bucket      the bucket name
     * @param key         the object key
     * @param data        the object content as a UTF-8 string
     * @param contentType the MIME type (e.g. "application/json")
     */
    void putObject(String bucket, String key, String data, String contentType);

    /**
     * Downloads an object from the store.
     *
     * @param bucket the bucket name
     * @param key    the object key
     * @return the object content as a UTF-8 string, or null if not found
     */
    String getObject(String bucket, String key);

    /**
     * Deletes an object from the store.
     *
     * @param bucket the bucket name
     * @param key    the object key
     */
    void deleteObject(String bucket, String key);

    /**
     * Lists object keys in a bucket with an optional prefix filter.
     *
     * @param bucket the bucket name
     * @param prefix the key prefix to filter by (empty string for all)
     * @return list of matching object keys
     */
    List<String> listObjects(String bucket, String prefix);

    /**
     * Checks whether an object exists in the store.
     *
     * @param bucket the bucket name
     * @param key    the object key
     * @return true if the object exists
     */
    boolean objectExists(String bucket, String key);
}
