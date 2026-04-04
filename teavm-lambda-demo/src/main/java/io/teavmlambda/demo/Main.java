package io.teavmlambda.demo;

import io.teavmlambda.adapter.lambda.LambdaAdapter;
import io.teavmlambda.core.Router;
import io.teavmlambda.db.Db;
import io.teavmlambda.db.PgPool;
import io.teavmlambda.generated.GeneratedRouter;
import org.teavm.jso.JSBody;

public class Main {

    @JSBody(params = {"name"}, script = "return process.env[name] || '';")
    private static native String getenv(String name);

    public static void main(String[] args) {
        String dbUrl = getenv("DATABASE_URL");
        if (dbUrl == null || dbUrl.isEmpty()) {
            dbUrl = "postgresql://demo:demo@localhost:5432/demo";
        }

        PgPool pool = PgPool.create(dbUrl);
        Db db = new Db(pool);

        UsersResource usersResource = new UsersResource(db);
        HealthResource healthResource = new HealthResource();

        Router router = new GeneratedRouter(healthResource, usersResource);
        LambdaAdapter.start(router);
    }
}
