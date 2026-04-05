package io.teavmlambda.adapter.lambda.jvm;

import io.teavmlambda.core.PlatformAdapter;
import io.teavmlambda.core.Router;

import java.util.logging.Logger;

/**
 * PlatformAdapter for AWS Lambda on JVM.
 * <p>
 * Note: On AWS Lambda JVM, the handler class is instantiated by the Lambda runtime,
 * so {@link #start(Router)} registers the router for the handler to use.
 * The user's Lambda handler class should extend {@link JvmLambdaAdapter}.
 */
public class LambdaJvmPlatformAdapter implements PlatformAdapter {

    private static final Logger logger = Logger.getLogger(LambdaJvmPlatformAdapter.class.getName());

    static volatile Router registeredRouter;

    @Override
    public String env(String name) {
        String val = System.getenv(name);
        return val != null ? val : "";
    }

    @Override
    public void start(Router router) {
        registeredRouter = router;
        logger.info("Router registered for Lambda JVM handler");
    }
}
