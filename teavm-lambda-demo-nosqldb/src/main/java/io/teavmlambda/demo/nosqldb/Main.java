package io.teavmlambda.demo.nosqldb;

import io.teavmlambda.adapter.lambda.LambdaAdapter;
import io.teavmlambda.core.Router;
import io.teavmlambda.dynamodb.DynamoNoSqlProvider;
import io.teavmlambda.firestore.FirestoreNoSqlProvider;
import io.teavmlambda.generated.GeneratedRouter;
import io.teavmlambda.nosqldb.NoSqlClient;
import io.teavmlambda.nosqldb.NoSqlClientFactory;
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
