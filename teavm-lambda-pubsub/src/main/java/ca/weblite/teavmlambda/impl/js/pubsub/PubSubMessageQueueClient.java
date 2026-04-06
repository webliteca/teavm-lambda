package ca.weblite.teavmlambda.impl.js.pubsub;

import ca.weblite.teavmlambda.api.messagequeue.Message;
import ca.weblite.teavmlambda.api.messagequeue.MessageQueueClient;
import org.teavm.jso.JSObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Google Cloud Pub/Sub implementation of MessageQueueClient.
 *
 * <p>Queue URL mapping:</p>
 * <ul>
 *   <li>For {@code sendMessage}: the queueUrl parameter is used as the topic name</li>
 *   <li>For {@code receiveMessages}/{@code deleteMessage}: it is the subscription name</li>
 * </ul>
 */
public class PubSubMessageQueueClient implements MessageQueueClient {

    private final JSObject pubsub;

    PubSubMessageQueueClient(JSObject pubsub) {
        this.pubsub = pubsub;
    }

    @Override
    public String sendMessage(String queueUrl, String messageBody) {
        JSObject result = PubSubAsyncBridge.await(
                PubSubJsBridge.publishMessage(pubsub, queueUrl, messageBody));
        return PubSubJsBridge.getStringProperty(result, "messageId");
    }

    @Override
    public List<Message> receiveMessages(String queueUrl, int maxMessages) {
        JSObject result = PubSubAsyncBridge.await(
                PubSubJsBridge.pullMessages(pubsub, queueUrl, maxMessages));

        List<Message> messages = new ArrayList<>();
        int len = PubSubJsBridge.arrayLength(result);
        for (int i = 0; i < len; i++) {
            JSObject msg = PubSubJsBridge.arrayGet(result, i);
            messages.add(new Message(
                    PubSubJsBridge.getStringProperty(msg, "messageId"),
                    PubSubJsBridge.getStringProperty(msg, "body"),
                    PubSubJsBridge.getStringProperty(msg, "receiptHandle")));
        }
        return messages;
    }

    @Override
    public void deleteMessage(String queueUrl, String receiptHandle) {
        PubSubAsyncBridge.awaitVoid(
                PubSubJsBridge.acknowledge(pubsub, queueUrl, receiptHandle));
    }

    @Override
    public int getMessageCount(String queueUrl) {
        // Pub/Sub does not natively support message count queries
        return -1;
    }
}
