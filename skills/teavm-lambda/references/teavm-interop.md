# TeaVM Interop & Classlib Guide

When to read this: you need to call a Node.js/JavaScript API that teavm-lambda doesn't already wrap, handle async JS operations, or you hit a compilation error because a Java API isn't available in TeaVM's classlib.

If you're building a standard REST API using teavm-lambda's built-in annotations, database, auth, and cloud service modules, **you don't need any of this** — the framework abstracts it away.

## @JSBody — Call JavaScript from Java

Embed raw JavaScript in a Java `static native` method. The `params` array maps Java parameters to JS variables available in the `script` string.

```java
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

// Simple: read an environment variable
@JSBody(params = {"name"}, script = "return process.env[name] || '';")
private static native String getEnv(String name);

// Require a Node.js module and call it
@JSBody(params = {"connectionString"}, script =
        "var pg = require('pg');"
        + "return new pg.Pool({ connectionString: connectionString });")
public static native PgPool create(String connectionString);

// Build a JS object from Java values
@JSBody(params = {"statusCode", "body", "headersArr"}, script =
        "var headers = {};"
        + "for (var i = 0; i < headersArr.length; i += 2) {"
        + "  headers[headersArr[i]] = headersArr[i + 1];"
        + "}"
        + "return { statusCode: statusCode, headers: headers, body: body || '' };")
private static native JSObject createApiGatewayResponse(int statusCode, String body, String[] headersArr);

// No params — use empty script
@JSBody(script = "return Date.now();")
private static native double now();
```

### Rules

| Rule | Detail |
|------|--------|
| Must be `static native` | Instance methods are not supported |
| `params` order must match Java parameter order | `params = {"a", "b"}` maps to `(String a, int b)` |
| Allowed parameter types | `String`, `int`, `double`, `boolean`, `byte[]`, `String[]`, `JSObject` (and subclasses) |
| Allowed return types | Same as parameters, plus `void` |
| No complex Java objects | You cannot pass a `Map`, `List`, or custom POJO — convert to `String[]` or `JSObject` first |
| String concatenation for multi-line | Use `"line1;" + "line2;"` — Java doesn't allow literal newlines in annotation values |
| `require()` works | Node.js modules are available since TeaVM output runs on Node.js |

## @JSFunctor — Pass Java Callbacks to JavaScript

Define a single-method interface extending `JSObject`. Instances can be passed to `@JSBody` methods as callback parameters.

```java
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

@JSFunctor
interface ResolveCallback extends JSObject {
    void resolve(JSObject value);
}

@JSFunctor
interface RejectCallback extends JSObject {
    void reject(JSObject error);
}

// Usage: pass functors to a @JSBody method
@JSBody(params = {"promise", "resolve", "reject"}, script =
        "promise.then(function(value) { resolve(value); })"
        + ".catch(function(err) { reject(err); });")
private static native void thenResolve(
        JSObject promise, ResolveCallback resolve, RejectCallback reject);
```

### Rules

- Must be an `interface` annotated with `@JSFunctor` that extends `JSObject`
- Exactly one abstract method (SAM interface)
- Parameter and return types follow the same rules as `@JSBody`
- Lambda expressions work: `resolve(value -> callback.complete(value))`

## JSObject Overlay Types — Wrap JS Objects in Java Interfaces

Use `@JSProperty` on getter/setter methods to map to JavaScript object properties.

```java
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;
import org.teavm.jso.core.JSArray;

// Interface overlay: maps to a JS object with .rows and .rowCount properties
public interface PgResult extends JSObject {
    @JSProperty
    JSArray<JSObject> getRows();

    @JSProperty
    int getRowCount();
}
```

For wrapping a Node.js module with chainable methods, use an `abstract class`:

```java
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSPromise;

public abstract class Sharp implements JSObject {

    @JSBody(params = {"buffer"}, script =
            "var sharp = require('sharp'); return sharp(buffer);")
    public static native Sharp create(JSObject buffer);

    @JSBody(params = {"instance", "width", "height"}, script =
            "return instance.resize(width, height, { fit: 'inside' });")
    public static native Sharp resizeFit(Sharp instance, int width, int height);

    @JSBody(params = {"instance"}, script = "return instance.toBuffer();")
    public static native JSPromise<JSObject> toBuffer(Sharp instance);
}
```

### Rules

- `interface` or `abstract class` must extend/implement `JSObject`
- `@JSProperty` maps `getFoo()` to `.foo` and `setFoo(x)` to `.foo = x`
- For methods that aren't simple property access, use `@JSBody` static methods passing the instance as first param

## @Async / AsyncCallback — Convert JS Promises to Synchronous Java

TeaVM's `@Async` mechanism suspends a TeaVM green thread until a callback fires, letting you write synchronous-looking Java code that awaits JavaScript Promises.

**This is the most important pattern.** Every teavm-lambda module that calls async JS APIs uses it.

```java
import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSPromise;

public final class AsyncBridge {

    // 1. Public method: @Async + native — this is what callers use
    @Async
    public static native JSObject await(JSPromise<? extends JSObject> promise);

    // 2. Private implementation: same name, same params + AsyncCallback at the end
    private static void await(JSPromise<? extends JSObject> promise,
                              AsyncCallback<JSObject> callback) {
        thenResolve(promise,
                value -> callback.complete(value),
                err -> callback.error(new RuntimeException(jsErrorToString(err))));
    }

    // Void variant for fire-and-forget operations (put, delete)
    @Async
    public static native void awaitVoid(JSPromise<? extends JSObject> promise);

    private static void awaitVoid(JSPromise<? extends JSObject> promise,
                                  AsyncCallback<Void> callback) {
        thenResolve(promise,
                value -> callback.complete(null),
                err -> callback.error(new RuntimeException(jsErrorToString(err))));
    }

    // Helper: wire Promise .then/.catch to @JSFunctor callbacks
    @JSBody(params = {"promise", "resolve", "reject"}, script =
            "promise.then(function(value) { resolve(value); })"
            + ".catch(function(err) { reject(err); });")
    private static native void thenResolve(
            JSObject promise, ResolveCallback resolve, RejectCallback reject);

    @JSBody(params = {"err"}, script =
            "return String(err && err.message ? err.message : err);")
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
```

### Rules

- The two methods **must** have the same name and the same parameter list, except the implementation adds `AsyncCallback<T>` as the final parameter
- The `@Async` method must be `static native`
- Call `callback.complete(value)` on success, `callback.error(exception)` on failure
- The calling Java code blocks (suspends the TeaVM green thread) until the callback fires — it looks like synchronous code to the caller
- Import from `org.teavm.interop.Async` and `org.teavm.interop.AsyncCallback`

## $rt_startThread — Run Java from JavaScript Callbacks

When JavaScript calls back into Java (e.g., an HTTP request handler), the code must run inside `$rt_startThread()` so TeaVM's thread scheduler is active. Without it, `@Async` calls will deadlock.

```java
@JSBody(params = {"javaHandler"}, script = ""
        + "function teavmHandler(event, context) {"
        + "  return new Promise(function(resolve, reject) {"
        + "    $rt_startThread(function() {"
        + "      javaHandler(event,"
        + "        function(result) { resolve(result); },"
        + "        function(err) { reject(err); }"
        + "      );"
        + "    });"
        + "  });"
        + "}"
        + "module.exports.handler = teavmHandler;")
private static native void exportHandler(RequestHandler javaHandler);
```

**When you need it:** any `@JSBody` script that receives a JavaScript callback (HTTP handler, event listener, timer) and needs to call Java code that uses `@Async`. The teavm-lambda adapters handle this for you — you only need it if writing a custom adapter.

## Buffer Conversion — Java byte[] ↔ Node.js Buffer

```java
// Java byte[] → Node.js Buffer
@JSBody(params = {"bytes"}, script = "return Buffer.from(bytes);")
public static native JSObject toNodeBuffer(byte[] bytes);

// Node.js Buffer → Java byte[]
@JSBody(params = {"buffer"}, script =
        "var arr = new Int8Array(buffer.buffer, buffer.byteOffset, buffer.length);"
        + "return Array.prototype.slice.call(arr);")
public static native byte[] fromNodeBuffer(JSObject buffer);
```

Use this pattern when working with binary data (images, file uploads, S3/GCS objects).

## TeaVM Classlib — What Works and What Doesn't

TeaVM implements a subset of the Java standard library. Code that compiles fine on JVM may fail during TeaVM compilation.

### Available (safe to use)

- `java.lang.*` — String, Math, Integer, StringBuilder, etc.
- `java.util.*` — HashMap, ArrayList, LinkedList, Arrays, Collections, Optional, Stream API
- `java.util.function.*` — Function, Consumer, Supplier, Predicate, etc.
- `java.util.regex.*` — Pattern, Matcher (uses JS regex engine under the hood)
- `java.io.IOException`, `java.io.InputStream`, `java.io.OutputStream` (limited)
- `java.util.Base64`
- `java.net.URLEncoder`, `java.net.URLDecoder`

### Not available (will fail TeaVM compilation)

| Java API | Why | Workaround |
|----------|-----|------------|
| `java.lang.reflect.*` | No reflection in TeaVM | Use teavm-lambda's compile-time DI and `JsonReader`/`JsonBuilder` |
| `java.nio.*` | Not implemented | Use `byte[]` and `InputStream` |
| `java.sql.*` / JDBC | Not in JS target | Use teavm-lambda-db (wraps Node.js pg via JSO) |
| `java.util.logging.*` | Partial/unreliable | Use teavm-lambda's `Logger` class |
| `java.net.HttpURLConnection` | Not in JS target | Use `@JSBody` to call Node.js `http`/`https` or `fetch` |
| `java.util.concurrent.*` | No real threads | Avoid — use `@Async` for async operations |
| `java.lang.Thread` | Single-threaded JS | TeaVM simulates threads via green threads for `@Async` only |
| `Class.forName()` | No reflection | Not possible — use compile-time wiring |
| Jackson / Gson | Reflection-based | Use `JsonReader.parse(str)` and `JsonBuilder.object()` |

### Regex differences

`java.util.regex` delegates to the JavaScript regex engine. Most patterns work identically, but:
- Lookbehinds with variable length may behave differently
- Named groups use JS syntax under the hood
- Unicode property escapes (`\p{L}`) have varying support

If you hit a regex edge case, test the pattern in both environments.

## Putting It All Together — Wrapping a New Node.js Library

Example: wrapping a hypothetical `slugify` npm package.

```java
package com.example.myapp;

import org.teavm.jso.JSBody;

/**
 * Wraps the Node.js "slugify" package.
 */
public final class Slugify {

    private Slugify() {}

    @JSBody(params = {"text"}, script =
            "var slugify = require('slugify');"
            + "return slugify(text, { lower: true, strict: true });")
    public static native String slugify(String text);
}
```

For async libraries, combine with the AsyncBridge pattern:

```java
package com.example.myapp;

import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.core.JSPromise;

public final class MyAsyncLib {

    private MyAsyncLib() {}

    /** Call this from Java — blocks until the JS promise resolves. */
    public static String fetchData(String url) {
        JSObject result = awaitPromise(doFetch(url));
        return resultToString(result);
    }

    @JSBody(params = {"url"}, script = "return require('node-fetch')(url);")
    private static native JSPromise<JSObject> doFetch(String url);

    @JSBody(params = {"response"}, script = "return JSON.stringify(response);")
    private static native String resultToString(JSObject response);

    @Async
    private static native JSObject awaitPromise(JSPromise<? extends JSObject> promise);

    private static void awaitPromise(JSPromise<? extends JSObject> promise,
                                     AsyncCallback<JSObject> callback) {
        thenResolve(promise,
                value -> callback.complete(value),
                err -> callback.error(new RuntimeException(jsErrorToString(err))));
    }

    @JSBody(params = {"promise", "resolve", "reject"}, script =
            "promise.then(function(v){resolve(v);}).catch(function(e){reject(e);});")
    private static native void thenResolve(
            JSObject promise, ResolveCallback resolve, RejectCallback reject);

    @JSBody(params = {"err"}, script =
            "return String(err && err.message ? err.message : err);")
    private static native String jsErrorToString(JSObject err);

    @JSFunctor interface ResolveCallback extends JSObject { void resolve(JSObject v); }
    @JSFunctor interface RejectCallback extends JSObject { void reject(JSObject e); }
}
```

**Don't forget:** add the npm package to your `docker/package.json` (or `src/main/resources/package.json`) so it's available at runtime.
