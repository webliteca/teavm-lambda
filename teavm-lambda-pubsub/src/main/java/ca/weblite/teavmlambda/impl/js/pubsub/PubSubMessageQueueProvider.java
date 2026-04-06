package ca.weblite.teavmlambda.impl.js.pubsub;

import ca.weblite.teavmlambda.api.messagequeue.MessageQueueClient;
import ca.weblite.teavmlambda.api.messagequeue.MessageQueueClientFactory;
import ca.weblite.teavmlambda.api.messagequeue.MessageQueueProvider;
import org.teavm.jso.JSObject;

/**
 * Message queue provider for Google Cloud Pub/Sub.
 *
 * <p>Connection URI formats:</p>
 * <ul>
 *   <li>{@code pubsub://my-project-id} - Pub/Sub for the given GCP project</li>
 *   <li>{@code pubsub://} - Pub/Sub using the default project from the environment</li>
 * </ul>
 */
public class PubSubMessageQueueProvider implements MessageQueueProvider {

    static {
        MessageQueueClientFactory.register(new PubSubMessageQueueProvider());
    }

    /**
     * Forces class initialization, which triggers provider registration.
     */
    public static void init() {
        // static initializer does the work
    }

    @Override
    public String getScheme() {
        return "pubsub";
    }

    @Override
    public MessageQueueClient create(String uri) {
        String projectId = uri.substring("pubsub://".length());
        JSObject pubsub = PubSubJsBridge.createClient(projectId);
        return new PubSubMessageQueueClient(pubsub);
    }
}
