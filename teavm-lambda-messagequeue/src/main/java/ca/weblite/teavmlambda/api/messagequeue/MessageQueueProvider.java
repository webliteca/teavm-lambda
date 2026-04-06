package ca.weblite.teavmlambda.api.messagequeue;

/**
 * SPI interface for message queue providers.
 * Each implementation module (SQS, Pub/Sub) registers a provider
 * that can create clients from a connection URI.
 */
public interface MessageQueueProvider {

    /**
     * Returns the URI scheme this provider handles (e.g. "sqs", "pubsub").
     */
    String getScheme();

    /**
     * Creates a MessageQueueClient from the given connection URI.
     *
     * @param uri the full connection URI (e.g. "sqs://us-east-1" or "pubsub://my-project")
     * @return a configured MessageQueueClient
     */
    MessageQueueClient create(String uri);
}
