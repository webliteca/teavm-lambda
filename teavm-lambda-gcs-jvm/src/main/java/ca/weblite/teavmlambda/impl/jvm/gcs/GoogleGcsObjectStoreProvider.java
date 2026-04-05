package ca.weblite.teavmlambda.impl.jvm.gcs;

import ca.weblite.teavmlambda.api.objectstore.ObjectStoreClient;
import ca.weblite.teavmlambda.api.objectstore.ObjectStoreProvider;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

/**
 * JVM-based ObjectStore provider for Google Cloud Storage.
 * Discovered via ServiceLoader.
 */
public class GoogleGcsObjectStoreProvider implements ObjectStoreProvider {

    @Override
    public String getScheme() {
        return "gcs";
    }

    @Override
    public ObjectStoreClient create(String uri) {
        String projectId = uri.substring("gcs://".length());
        StorageOptions.Builder builder = StorageOptions.newBuilder();
        if (projectId != null && !projectId.isEmpty()) {
            builder.setProjectId(projectId);
        }
        Storage storage = builder.build().getService();
        return new GoogleGcsObjectStoreClient(storage);
    }
}
