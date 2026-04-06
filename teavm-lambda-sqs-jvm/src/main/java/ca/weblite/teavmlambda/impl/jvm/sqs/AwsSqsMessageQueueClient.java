package ca.weblite.teavmlambda.impl.jvm.sqs;

import ca.weblite.teavmlambda.api.messagequeue.Message;
import ca.weblite.teavmlambda.api.messagequeue.MessageQueueClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * JVM implementation of MessageQueueClient using the AWS SDK for Java v2.
 */
public class AwsSqsMessageQueueClient implements MessageQueueClient {

    private final SqsClient sqs;

    AwsSqsMessageQueueClient(SqsClient sqs) {
        this.sqs = sqs;
    }

    @Override
    public String sendMessage(String queueUrl, String messageBody) {
        SendMessageResponse response = sqs.sendMessage(
                SendMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .messageBody(messageBody)
                        .build());
        return response.messageId();
    }

    @Override
    public List<Message> receiveMessages(String queueUrl, int maxMessages) {
        ReceiveMessageResponse response = sqs.receiveMessage(
                ReceiveMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .maxNumberOfMessages(maxMessages)
                        .waitTimeSeconds(0)
                        .build());

        List<Message> messages = new ArrayList<>();
        for (software.amazon.awssdk.services.sqs.model.Message msg : response.messages()) {
            messages.add(new Message(msg.messageId(), msg.body(), msg.receiptHandle()));
        }
        return messages;
    }

    @Override
    public void deleteMessage(String queueUrl, String receiptHandle) {
        sqs.deleteMessage(
                DeleteMessageRequest.builder()
                        .queueUrl(queueUrl)
                        .receiptHandle(receiptHandle)
                        .build());
    }

    @Override
    public int getMessageCount(String queueUrl) {
        GetQueueAttributesResponse response = sqs.getQueueAttributes(
                GetQueueAttributesRequest.builder()
                        .queueUrl(queueUrl)
                        .attributeNames(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES)
                        .build());
        String count = response.attributes().get(QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES);
        if (count != null) {
            try {
                return Integer.parseInt(count);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }
}
