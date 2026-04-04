package io.teavmlambda.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class Response {

    private final int statusCode;
    private final Map<String, String> headers;
    private final String body;

    private Response(int statusCode, Map<String, String> headers, String body) {
        this.statusCode = statusCode;
        this.headers = Collections.unmodifiableMap(new HashMap<>(headers));
        this.body = body;
    }

    public static Response ok(String body) {
        return new Response(200, Collections.emptyMap(), body);
    }

    public static Response status(int statusCode) {
        return new Response(statusCode, Collections.emptyMap(), null);
    }

    public Response header(String name, String value) {
        Map<String, String> newHeaders = new HashMap<>(this.headers);
        newHeaders.put(name, value);
        return new Response(this.statusCode, newHeaders, this.body);
    }

    public Response body(String body) {
        return new Response(this.statusCode, this.headers, body);
    }

    public int getStatusCode() {
        return statusCode;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }
}
