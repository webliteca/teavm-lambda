package io.teavmlambda.adapter.cloudrun.jvm;

import io.teavmlambda.core.PlatformAdapter;
import io.teavmlambda.core.Router;

/**
 * PlatformAdapter for Cloud Run / standalone HTTP server on JVM.
 * Discovered via ServiceLoader or installed explicitly.
 */
public class CloudRunJvmPlatformAdapter implements PlatformAdapter {

    @Override
    public String env(String name) {
        String val = System.getenv(name);
        return val != null ? val : "";
    }

    @Override
    public void start(Router router) {
        JvmCloudRunAdapter.start(router);
    }
}
