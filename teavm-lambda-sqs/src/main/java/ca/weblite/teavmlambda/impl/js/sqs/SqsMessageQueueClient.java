package ca.weblite.teavmlambda.impl.js.sqs;

import ca.weblite.teavmlambda.api.messagequeue.Message;
import ca.weblite.teavmlambda.api.messagequeue.MessageQueueClient;
import org.teavm.jso.JSObject;

import java.util.ArrayList;
import java.util.List;

/**
 * AWS SQS implementation of MessageQueueClient using the AWS SDK v3 for JavaScript.
 */
public class SqsMessageQueueClient implements MessageQueueClient {

    private final JSObject sqsClient;

    SqsMessageQueueClient(JSObject sqsClient) {
        this.sqsClient = sqsClient;
    }

    @Override
    public String sendMessage(String queueUrl, String messageBody) {
        JSObject result = SqsAsyncBridge.await(
                SqsJsBridge.sendMessage(sqsClient, queueUrl, messageBody));
        return SqsJsBridge.getStringProperty(result, "MessageId");
    }

    @Override
    public List<Message> receiveMessages(String queueUrl, int maxMessages) {
        JSObject result = SqsAsyncBridge.await(
                SqsJsBridge.receiveMessages(sqsClient, queueUrl, maxMessages));

        List<Message> messages = new ArrayList<>();
        if (SqsJsBridge.hasProperty(result, "Messages")) {
            JSObject msgs = SqsJsBridge.getProperty(result, "Messages");
            int len = SqsJsBridge.arrayLength(msgs);
            for (int i = 0; i < len; i++) {
                JSObject msg = SqsJsBridge.arrayGet(msgs, i);
                messages.add(new Message(
                        SqsJsBridge.getStringProperty(msg, "MessageId"),
                        SqsJsBridge.getStringProperty(msg, "Body"),
                        SqsJsBridge.getStringProperty(msg, "ReceiptHandle")));
            }
        }
        return messages;
    }

    @Override
    public void deleteMessage(String queueUrl, String receiptHandle) {
        SqsAsyncBridge.awaitVoid(
                SqsJsBridge.deleteMessage(sqsClient, queueUrl, receiptHandle));
    }

    @Override
    public int getMessageCount(String queueUrl) {
        JSObject result = SqsAsyncBridge.await(
                SqsJsBridge.getQueueAttributes(sqsClient, queueUrl));
        if (SqsJsBridge.hasProperty(result, "Attributes")) {
            JSObject attrs = SqsJsBridge.getProperty(result, "Attributes");
            String count = SqsJsBridge.getStringProperty(attrs, "ApproximateNumberOfMessages");
            try {
                return Integer.parseInt(count);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }
}
