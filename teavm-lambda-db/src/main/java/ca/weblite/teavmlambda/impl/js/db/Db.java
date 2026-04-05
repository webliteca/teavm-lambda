package ca.weblite.teavmlambda.impl.js.db;

import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSPromise;

public class Db {

    private final PgPool pool;

    public Db(PgPool pool) {
        this.pool = pool;
    }

    public PgResult query(String sql) {
        return awaitPgPromise(PgPool.queryRaw(pool, sql));
    }

    public PgResult query(String sql, String... params) {
        JSObject jsParams = JsUtil.toJsArray(params);
        return awaitPgPromise(PgPool.queryRaw(pool, sql, jsParams));
    }

    @Async
    private static native PgResult awaitPgPromise(JSPromise<PgResult> promise);

    private static void awaitPgPromise(JSPromise<PgResult> promise, AsyncCallback<PgResult> callback) {
        thenResolve(promise,
                value -> callback.complete((PgResult) value),
                err -> callback.error(new RuntimeException(jsErrorToString(err))));
    }

    @JSBody(params = {"promise", "resolve", "reject"}, script =
            "promise.then(function(value) { resolve(value); })"
            + ".catch(function(err) { reject(err); });")
    private static native void thenResolve(JSObject promise, ResolveCallback resolve, RejectCallback reject);

    @JSBody(params = {"err"}, script = "return String(err && err.message ? err.message : err);")
    private static native String jsErrorToString(JSObject err);

    @JSFunctor
    interface ResolveCallback extends JSObject {
        void resolve(JSObject value);
    }

    @JSFunctor
    interface RejectCallback extends JSObject {
        void reject(JSObject error);
    }
}
