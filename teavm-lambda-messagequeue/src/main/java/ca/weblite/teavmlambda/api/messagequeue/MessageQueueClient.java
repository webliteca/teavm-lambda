package ca.weblite.teavmlambda.api.messagequeue;

import java.util.List;

/**
 * Platform-neutral interface for message queue operations.
 *
 * <p>Implementations are provided by teavm-lambda-sqs (AWS SQS) and
 * teavm-lambda-pubsub (Google Cloud Pub/Sub).</p>
 */
public interface MessageQueueClient {

    /**
     * Sends a message to the queue.
     *
     * @param queueUrl    the queue URL or topic name
     * @param messageBody the message content
     * @return the message ID assigned by the service
     */
    String sendMessage(String queueUrl, String messageBody);

    /**
     * Receives messages from the queue.
     *
     * @param queueUrl    the queue URL or subscription name
     * @param maxMessages maximum number of messages to receive (1-10)
     * @return list of received messages
     */
    List<Message> receiveMessages(String queueUrl, int maxMessages);

    /**
     * Deletes/acknowledges a message so it is not delivered again.
     *
     * @param queueUrl      the queue URL or subscription name
     * @param receiptHandle the receipt handle from a received message
     */
    void deleteMessage(String queueUrl, String receiptHandle);

    /**
     * Returns the approximate number of messages in the queue.
     *
     * @param queueUrl the queue URL or subscription name
     * @return approximate message count, or -1 if unsupported
     */
    int getMessageCount(String queueUrl);
}
