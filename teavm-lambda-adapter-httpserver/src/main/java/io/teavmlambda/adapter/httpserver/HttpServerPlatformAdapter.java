package io.teavmlambda.adapter.httpserver;

import io.teavmlambda.core.PlatformAdapter;
import io.teavmlambda.core.Router;

/**
 * PlatformAdapter backed by JDK's built-in HTTP server.
 * Works anywhere a JVM runs: Cloud Run, Docker, bare metal, local dev.
 * Discovered via ServiceLoader or installed explicitly.
 */
public class HttpServerPlatformAdapter implements PlatformAdapter {

    @Override
    public String env(String name) {
        String val = System.getenv(name);
        return val != null ? val : "";
    }

    @Override
    public void start(Router router) {
        HttpServerAdapter.start(router);
    }
}
