package ca.weblite.teavmlambda.impl.js.gcs;

import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSPromise;

/**
 * Bridge for converting JavaScript Promises to synchronous Java calls
 * using TeaVM's @Async mechanism.
 */
final class GcsAsyncBridge {

    private GcsAsyncBridge() {
    }

    @Async
    static native JSObject await(JSPromise<? extends JSObject> promise);

    private static void await(JSPromise<? extends JSObject> promise, AsyncCallback<JSObject> callback) {
        thenResolve(promise,
                value -> callback.complete(value),
                err -> callback.error(new RuntimeException(jsErrorToString(err))));
    }

    @Async
    static native void awaitVoid(JSPromise<? extends JSObject> promise);

    private static void awaitVoid(JSPromise<? extends JSObject> promise, AsyncCallback<Void> callback) {
        thenResolve(promise,
                value -> callback.complete(null),
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
