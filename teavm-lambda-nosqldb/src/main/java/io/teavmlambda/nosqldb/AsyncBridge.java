package io.teavmlambda.nosqldb;

import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSPromise;

/**
 * Bridge for converting JavaScript Promises to synchronous Java calls
 * using TeaVM's @Async mechanism. Shared across all NoSQL provider implementations.
 */
public final class AsyncBridge {

    private AsyncBridge() {
    }

    /**
     * Blocks the current TeaVM thread until the given promise resolves.
     *
     * @param promise a JS promise that resolves to a JSObject
     * @return the resolved value
     * @throws RuntimeException if the promise rejects
     */
    @Async
    public static native JSObject await(JSPromise<? extends JSObject> promise);

    private static void await(JSPromise<? extends JSObject> promise, AsyncCallback<JSObject> callback) {
        thenResolve(promise,
                value -> callback.complete(value),
                err -> callback.error(new RuntimeException(jsErrorToString(err))));
    }

    /**
     * Blocks until the given promise resolves, discarding the result.
     * Useful for put/delete operations that return void.
     */
    @Async
    public static native void awaitVoid(JSPromise<? extends JSObject> promise);

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
