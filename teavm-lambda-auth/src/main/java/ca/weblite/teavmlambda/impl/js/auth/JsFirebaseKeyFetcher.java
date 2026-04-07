package ca.weblite.teavmlambda.impl.js.auth;

import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSPromise;

/**
 * Fetches and caches Firebase/Google public keys for RS256 JWT verification.
 * Uses Node.js {@code https} module to fetch X.509 certificates from Google's endpoint.
 * Keys are cached based on the {@code Cache-Control: max-age} header.
 */
final class JsFirebaseKeyFetcher {

    private static final String GOOGLE_CERTS_URL =
            "https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com";

    /** Cached JSON string of kid->PEM mappings */
    private static String cachedKeysJson;
    /** Epoch millis when cache expires */
    private static long cacheExpiresAt;

    private JsFirebaseKeyFetcher() {
    }

    /**
     * Returns the PEM-encoded public key for the given key ID.
     * Fetches from Google if the cache is expired.
     *
     * @param kid the key ID from the JWT header
     * @return PEM-encoded X.509 certificate, or null if kid not found
     */
    static String getPublicKey(String kid) {
        long now = System.currentTimeMillis();
        if (cachedKeysJson == null || now >= cacheExpiresAt) {
            refreshKeys();
        }
        if (cachedKeysJson == null) {
            return null;
        }
        return extractJsonString(cachedKeysJson, kid);
    }

    /**
     * Allows tests to inject keys without hitting the network.
     */
    static void setKeysForTesting(String keysJson, long expiresAt) {
        cachedKeysJson = keysJson;
        cacheExpiresAt = expiresAt;
    }

    /**
     * Clears the cached keys (useful for testing).
     */
    static void clearCache() {
        cachedKeysJson = null;
        cacheExpiresAt = 0;
    }

    private static void refreshKeys() {
        try {
            JSObject result = awaitFetch(fetchKeys(GOOGLE_CERTS_URL));
            String body = getBody(result);
            long maxAge = getMaxAge(result);
            if (body != null && !body.isEmpty()) {
                cachedKeysJson = body;
                cacheExpiresAt = System.currentTimeMillis() + (maxAge * 1000);
            }
        } catch (RuntimeException e) {
            // If fetch fails and we have cached keys, continue using them
            if (cachedKeysJson == null) {
                throw e;
            }
        }
    }

    // --- Async bridge for HTTPS fetch ---

    @Async
    private static native JSObject awaitFetch(JSPromise<? extends JSObject> promise);

    private static void awaitFetch(JSPromise<? extends JSObject> promise, AsyncCallback<JSObject> callback) {
        thenResolve(promise,
                value -> callback.complete(value),
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

    // --- JS interop for HTTPS fetch ---

    @JSBody(params = {"url"}, script = ""
            + "return new Promise(function(resolve, reject) {"
            + "  var https = require('https');"
            + "  https.get(url, function(res) {"
            + "    var data = '';"
            + "    var maxAge = 21600;" // default 6 hours
            + "    var cc = res.headers['cache-control'];"
            + "    if (cc) {"
            + "      var m = cc.match(/max-age=(\\d+)/);"
            + "      if (m) maxAge = parseInt(m[1], 10);"
            + "    }"
            + "    res.on('data', function(chunk) { data += chunk; });"
            + "    res.on('end', function() {"
            + "      resolve({ body: data, maxAge: maxAge });"
            + "    });"
            + "  }).on('error', function(err) {"
            + "    reject(err);"
            + "  });"
            + "});")
    private static native JSPromise<JSObject> fetchKeys(String url);

    @JSBody(params = {"result"}, script = "return result.body || '';")
    private static native String getBody(JSObject result);

    @JSBody(params = {"result"}, script = "return result.maxAge || 21600;")
    private static native long getMaxAge(JSObject result);

    @JSBody(params = {"json", "key"}, script = ""
            + "try { var obj = JSON.parse(json); return obj[key] != null ? String(obj[key]) : null; }"
            + "catch(e) { return null; }")
    private static native String extractJsonString(String json, String key);
}
