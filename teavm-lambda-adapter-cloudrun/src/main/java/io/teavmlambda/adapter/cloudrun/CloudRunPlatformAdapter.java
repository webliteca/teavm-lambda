package io.teavmlambda.adapter.cloudrun;

import io.teavmlambda.core.PlatformAdapter;
import io.teavmlambda.core.Router;
import org.teavm.jso.JSBody;

/**
 * PlatformAdapter for Cloud Run on Node.js/TeaVM.
 * Discovered via ServiceLoader or installed explicitly.
 */
public class CloudRunPlatformAdapter implements PlatformAdapter {

    @Override
    public String env(String name) {
        return getEnv(name);
    }

    @Override
    public void start(Router router) {
        CloudRunAdapter.start(router);
    }

    @JSBody(params = {"name"}, script = "return process.env[name] || '';")
    private static native String getEnv(String name);
}
