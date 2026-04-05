package ca.weblite.teavmlambda.demo.objectstore;

import ca.weblite.teavmlambda.impl.js.lambda.LambdaAdapter;
import ca.weblite.teavmlambda.api.Router;
import ca.weblite.teavmlambda.impl.js.s3.S3ObjectStoreProvider;
import ca.weblite.teavmlambda.impl.js.gcs.GcsObjectStoreProvider;
import ca.weblite.teavmlambda.generated.GeneratedRouter;
import ca.weblite.teavmlambda.api.objectstore.ObjectStoreClient;
import ca.weblite.teavmlambda.api.objectstore.ObjectStoreClientFactory;
import org.teavm.jso.JSBody;

public class Main {

    @JSBody(params = {"name"}, script = "return process.env[name] || '';")
    private static native String getenv(String name);

    public static void main(String[] args) {
        // Register object store providers
        S3ObjectStoreProvider.init();
        GcsObjectStoreProvider.init();

        // Connection URI determines which backend is used:
        //   s3://us-east-1            -> S3
        //   s3://localhost:9000       -> MinIO (S3-compatible)
        //   gcs://my-project-id      -> Google Cloud Storage
        String objectStoreUri = getenv("OBJECTSTORE_URI");
        if (objectStoreUri == null || objectStoreUri.isEmpty()) {
            objectStoreUri = "s3://us-east-1";
        }

        String bucket = getenv("OBJECTSTORE_BUCKET");
        if (bucket == null || bucket.isEmpty()) {
            bucket = "demo-bucket";
        }

        ObjectStoreClient client = ObjectStoreClientFactory.create(objectStoreUri);

        FilesResource filesResource = new FilesResource(client, bucket);
        HealthResource healthResource = new HealthResource();

        Router router = new GeneratedRouter(filesResource, healthResource);
        LambdaAdapter.start(router);
    }
}
