package io.teavmlambda.db;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSPromise;

public abstract class PgPool implements JSObject {

    @JSBody(params = {"connectionString"}, script =
            "var pg = require('pg');"
            + "return new pg.Pool({ connectionString: connectionString });")
    public static native PgPool create(String connectionString);

    @JSBody(params = {"config"}, script =
            "var pg = require('pg');"
            + "return new pg.Pool(config);")
    public static native PgPool createWithConfig(JSObject config);

    @JSBody(params = {"pool", "sql"}, script = "return pool.query(sql);")
    public static native JSPromise<PgResult> queryRaw(PgPool pool, String sql);

    @JSBody(params = {"pool", "sql", "params"}, script = "return pool.query(sql, params);")
    public static native JSPromise<PgResult> queryRaw(PgPool pool, String sql, JSObject params);

    @JSBody(params = {"pool"}, script = "return pool.end();")
    public static native JSPromise<JSObject> end(PgPool pool);
}
