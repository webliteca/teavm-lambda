package io.teavmlambda.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class Request {

    private final String method;
    private final String path;
    private final Map<String, String> headers;
    private final Map<String, String> queryParams;
    private final Map<String, String> pathParams;
    private final String body;

    public Request(String method, String path, Map<String, String> headers,
                   Map<String, String> queryParams, String body) {
        this(method, path, headers, queryParams, Collections.emptyMap(), body);
    }

    public Request(String method, String path, Map<String, String> headers,
                   Map<String, String> queryParams, Map<String, String> pathParams, String body) {
        this.method = method;
        this.path = path;
        this.headers = headers != null ? Collections.unmodifiableMap(new HashMap<>(headers)) : Collections.emptyMap();
        this.queryParams = queryParams != null ? Collections.unmodifiableMap(new HashMap<>(queryParams)) : Collections.emptyMap();
        this.pathParams = pathParams != null ? Collections.unmodifiableMap(new HashMap<>(pathParams)) : Collections.emptyMap();
        this.body = body;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public Map<String, String> getPathParams() {
        return pathParams;
    }

    public String getBody() {
        return body;
    }

    public Request withPathParams(Map<String, String> pathParams) {
        return new Request(method, path, headers, queryParams, pathParams, body);
    }
}
