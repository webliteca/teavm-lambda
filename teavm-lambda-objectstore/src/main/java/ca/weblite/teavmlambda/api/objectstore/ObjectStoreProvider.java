package ca.weblite.teavmlambda.api.objectstore;

/**
 * SPI interface for object storage providers.
 * Each implementation module (S3, GCS) registers a provider
 * that can create clients from a connection URI.
 */
public interface ObjectStoreProvider {

    /**
     * Returns the URI scheme this provider handles (e.g. "s3", "gcs").
     */
    String getScheme();

    /**
     * Creates an ObjectStoreClient from the given connection URI.
     *
     * @param uri the full connection URI (e.g. "s3://us-east-1" or "gcs://my-project")
     * @return a configured ObjectStoreClient
     */
    ObjectStoreClient create(String uri);
}
