package ca.weblite.teavmlambda.demo.nosqldb;

import ca.weblite.teavmlambda.impl.js.lambda.LambdaAdapter;
import ca.weblite.teavmlambda.api.Router;
import ca.weblite.teavmlambda.impl.js.dynamodb.DynamoNoSqlProvider;
import ca.weblite.teavmlambda.impl.js.firestore.FirestoreNoSqlProvider;
import ca.weblite.teavmlambda.generated.GeneratedRouter;
import ca.weblite.teavmlambda.api.nosqldb.NoSqlClient;
import ca.weblite.teavmlambda.api.nosqldb.NoSqlClientFactory;
import org.teavm.jso.JSBody;

public class Main {

    @JSBody(params = {"name"}, script = "return process.env[name] || '';")
    private static native String getenv(String name);

    public static void main(String[] args) {
        // Register NoSQL providers
        DynamoNoSqlProvider.init();
        FirestoreNoSqlProvider.init();

        // Connection URI determines which backend is used:
        //   dynamodb://us-east-1          -> DynamoDB
        //   dynamodb://localhost:8000     -> DynamoDB Local
        //   firestore://my-project-id    -> Firestore
        String nosqlUri = getenv("NOSQL_URI");
        if (nosqlUri == null || nosqlUri.isEmpty()) {
            nosqlUri = "dynamodb://us-east-1";
        }

        NoSqlClient client = NoSqlClientFactory.create(nosqlUri);

        UsersResource usersResource = new UsersResource(client);
        HealthResource healthResource = new HealthResource();

        Router router = new GeneratedRouter(healthResource, usersResource);
        LambdaAdapter.start(router);
    }
}
