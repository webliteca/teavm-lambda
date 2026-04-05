package ca.weblite.teavmlambda.impl.js.firestore;

import ca.weblite.teavmlambda.api.nosqldb.NoSqlClient;
import ca.weblite.teavmlambda.api.nosqldb.NoSqlClientFactory;
import ca.weblite.teavmlambda.api.nosqldb.NoSqlProvider;
import org.teavm.jso.JSObject;

/**
 * NoSQL provider for Google Cloud Firestore.
 *
 * <p>Connection URI formats:</p>
 * <ul>
 *   <li>{@code firestore://my-project-id} - Firestore for the given GCP project</li>
 *   <li>{@code firestore://} - Firestore using the default project from the environment</li>
 * </ul>
 */
public class FirestoreNoSqlProvider implements NoSqlProvider {

    static {
        NoSqlClientFactory.register(new FirestoreNoSqlProvider());
    }

    /**
     * Forces class initialization, which triggers provider registration.
     * Call this from your main() before using NoSqlClientFactory.
     */
    public static void init() {
        // static initializer does the work
    }

    @Override
    public String getScheme() {
        return "firestore";
    }

    @Override
    public NoSqlClient create(String uri) {
        // Parse: firestore://[project-id]
        String projectId = uri.substring("firestore://".length());
        JSObject db = FirestoreJsBridge.createClient(projectId);
        return new FirestoreNoSqlClient(db);
    }
}
