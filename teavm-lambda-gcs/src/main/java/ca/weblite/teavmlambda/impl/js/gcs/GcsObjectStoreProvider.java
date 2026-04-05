package ca.weblite.teavmlambda.impl.js.gcs;

import ca.weblite.teavmlambda.api.objectstore.ObjectStoreClient;
import ca.weblite.teavmlambda.api.objectstore.ObjectStoreClientFactory;
import ca.weblite.teavmlambda.api.objectstore.ObjectStoreProvider;
import org.teavm.jso.JSObject;

/**
 * Object store provider for Google Cloud Storage.
 *
 * <p>Connection URI formats:</p>
 * <ul>
 *   <li>{@code gcs://my-project-id} - GCS for the given GCP project</li>
 *   <li>{@code gcs://} - GCS using the default project from the environment</li>
 * </ul>
 */
public class GcsObjectStoreProvider implements ObjectStoreProvider {

    static {
        ObjectStoreClientFactory.register(new GcsObjectStoreProvider());
    }

    /**
     * Forces class initialization, which triggers provider registration.
     */
    public static void init() {
        // static initializer does the work
    }

    @Override
    public String getScheme() {
        return "gcs";
    }

    @Override
    public ObjectStoreClient create(String uri) {
        String projectId = uri.substring("gcs://".length());
        JSObject storage = GcsJsBridge.createClient(projectId);
        return new GcsObjectStoreClient(storage);
    }
}
