package ca.weblite.teavmlambda.impl.js.s3;

import ca.weblite.teavmlambda.api.objectstore.ObjectStoreClient;
import ca.weblite.teavmlambda.api.objectstore.ObjectStoreClientFactory;
import ca.weblite.teavmlambda.api.objectstore.ObjectStoreProvider;
import org.teavm.jso.JSObject;

/**
 * Object store provider for Amazon S3.
 *
 * <p>Connection URI formats:</p>
 * <ul>
 *   <li>{@code s3://us-east-1} - S3 in us-east-1</li>
 *   <li>{@code s3://localhost:9000} - MinIO / S3-compatible local endpoint</li>
 * </ul>
 */
public class S3ObjectStoreProvider implements ObjectStoreProvider {

    static {
        ObjectStoreClientFactory.register(new S3ObjectStoreProvider());
    }

    /**
     * Forces class initialization, which triggers provider registration.
     */
    public static void init() {
        // static initializer does the work
    }

    @Override
    public String getScheme() {
        return "s3";
    }

    @Override
    public ObjectStoreClient create(String uri) {
        String remainder = uri.substring("s3://".length());
        String hostOrRegion = remainder;

        String region;
        String endpoint = null;
        boolean forcePathStyle = false;

        if (hostOrRegion.contains(":")) {
            endpoint = "http://" + hostOrRegion;
            region = "us-east-1";
            forcePathStyle = true;
        } else {
            region = hostOrRegion;
        }

        JSObject client = S3JsBridge.createClient(region, endpoint, forcePathStyle);
        return new S3ObjectStoreClient(client);
    }
}
