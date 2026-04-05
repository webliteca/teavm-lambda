package ca.weblite.teavmlambda.impl.jvm.pubsub;

import ca.weblite.teavmlambda.api.messagequeue.MessageQueueClient;
import ca.weblite.teavmlambda.api.messagequeue.MessageQueueProvider;

/**
 * JVM-based MessageQueue provider for Google Cloud Pub/Sub.
 * Discovered via ServiceLoader.
 */
public class GooglePubSubMessageQueueProvider implements MessageQueueProvider {

    @Override
    public String getScheme() {
        return "pubsub";
    }

    @Override
    public MessageQueueClient create(String uri) {
        String projectId = uri.substring("pubsub://".length());
        return new GooglePubSubMessageQueueClient(projectId);
    }
}
