package ca.weblite.teavmlambda.impl.js.lambda;

import ca.weblite.teavmlambda.api.PlatformAdapter;
import ca.weblite.teavmlambda.api.Router;
import org.teavm.jso.JSBody;

/**
 * PlatformAdapter for AWS Lambda on Node.js/TeaVM.
 * Discovered via ServiceLoader or installed explicitly.
 */
public class LambdaPlatformAdapter implements PlatformAdapter {

    @Override
    public String env(String name) {
        return getEnv(name);
    }

    @Override
    public void start(Router router) {
        LambdaAdapter.start(router);
    }

    @JSBody(params = {"name"}, script = "return process.env[name] || '';")
    private static native String getEnv(String name);
}
